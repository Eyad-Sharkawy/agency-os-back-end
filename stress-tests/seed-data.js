import http from 'k6/http';
import { check } from 'k6';
import encoding from 'k6/encoding';

// Single Virtual User running sequentially to seed structured records
export const options = {
  vus: 1,
  iterations: __ENV.COUNT ? parseInt(__ENV.COUNT) : 50, // Default to 50 records
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080';

// Helper function to decode Keycloak User ID (sub) from JWT
function getUserIdFromToken(token) {
  try {
    const parts = token.split('.');
    if (parts.length < 2) return 'user-stress-test';
    const decoded = encoding.b64decode(parts[1], 'rawurl', 's');
    const payload = JSON.parse(decoded);
    return payload.sub || 'user-stress-test';
  } catch (e) {
    return 'user-stress-test';
  }
}

export function setup() {
  const tokensStr = __ENV.JWT_TOKENS || __ENV.JWT_TOKEN || '';
  if (!tokensStr) {
    throw new Error("JWT_TOKEN or JWT_TOKENS environment variable is required!");
  }

  const tokens = tokensStr.split(',').map(t => t.trim());
  const usersList = tokens.map((token, index) => {
    return {
      token: token,
      userId: getUserIdFromToken(token),
      username: `test_user_${index + 1}`
    };
  });

  // 0. Self-registration: Forces the backend to register/sync each Keycloak user to the local AppUser table
  console.log("Registering all 5 test users in the database...");
  for (let i = 0; i < usersList.length; i++) {
    const user = usersList[i];
    const headers = {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${user.token}`,
    };

    let tenantId = '';
    // Query workspaces to check if an OWNER workspace already exists
    const listRes = http.get(`${BASE_URL}/api/v1/workspaces`, { headers });
    if (listRes.status === 200) {
      const workspaces = JSON.parse(listRes.body);
      const ownerWorkspace = workspaces.find(w => w.role === 'OWNER');
      if (ownerWorkspace) {
        tenantId = ownerWorkspace.tenantId;
      }
    }

    // Only create a new workspace if the user doesn't already own one
    if (!tenantId) {
      const regRes = http.post(`${BASE_URL}/api/v1/workspaces`, JSON.stringify({ name: `Registration Workspace ${user.username}` }), { headers });
      console.log(`Syncing user ${user.username} -> status: ${regRes.status}`);
    } else {
      console.log(`User ${user.username} already owns workspace: ${tenantId}`);
    }
  }

  const TENANTS_ENV = __ENV.TENANT_IDS || __ENV.TENANT_ID || '';
  if (TENANTS_ENV) {
    return {
      tenants: TENANTS_ENV.split(',').map(s => s.trim()),
      users: usersList
    };
  }

  const ownerHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${usersList[0].token}`,
  };

  // 1. Fetch user's existing workspaces (populated by the seeder) using owner's token
  console.log("No TENANT_IDS provided. Querying active user workspaces...");
  const listRes = http.get(`${BASE_URL}/api/v1/workspaces`, { headers: ownerHeaders });
  if (listRes.status === 200) {
    const workspaces = JSON.parse(listRes.body);
    if (workspaces.length > 0) {
      const activeTenants = workspaces.map(w => w.tenantId);
      console.log(`Found active workspaces: ${JSON.stringify(activeTenants)}. Ensuring teammate memberships...`);

      // Self-healing: Ensure all other users are invited and joined to these workspaces
      for (const tenantId of activeTenants) {
        const currentWorkspace = workspaces.find(w => w.tenantId === tenantId);
        const workspaceUuid = currentWorkspace ? currentWorkspace.id : null;

        for (let i = 1; i < usersList.length; i++) {
          const invitee = usersList[i];
          const inviteeHeaders = {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${invitee.token}`,
          };

          // Check if invitee is already a member of this tenant workspace
          let alreadyMember = false;
          const inviteeWorkspacesRes = http.get(`${BASE_URL}/api/v1/workspaces`, { headers: inviteeHeaders });
          if (inviteeWorkspacesRes.status === 200) {
            const workspaces = JSON.parse(inviteeWorkspacesRes.body);
            alreadyMember = workspaces.some(w => w.tenantId === tenantId);
          }

          if (!alreadyMember) {
            if (i === 4) {
              // Invite test_user_5 (index 4) as CLIENT
              console.log(`User ${invitee.username} is not a member of ${tenantId}. Setting up client and sending CLIENT invitation...`);
              
              const tenantHeaders = {
                'Content-Type': 'application/json',
                'X-Tenant-ID': tenantId,
                'Authorization': `Bearer ${usersList[0].token}`,
              };

              let clientId = '';
              const clientsRes = http.get(`${BASE_URL}/api/v1/clients`, { headers: tenantHeaders });
              if (clientsRes.status === 200) {
                const clients = JSON.parse(clientsRes.body);
                const existingClient = clients.find(c => c.name === "Default Client for Portal Testing");
                if (existingClient) {
                  clientId = existingClient.id;
                }
              }

              if (!clientId) {
                const clientRes = http.post(`${BASE_URL}/api/v1/clients`, JSON.stringify({
                  name: "Default Client for Portal Testing",
                  email: "portal@test.com",
                  status: "ACTIVE"
                }), { headers: tenantHeaders });
                if (clientRes.status === 201) {
                  const client = JSON.parse(clientRes.body);
                  clientId = client.id;
                }
              }

              if (clientId) {
                const inviteRes = http.post(`${BASE_URL}/api/v1/workspaces/${tenantId}/invitations`, JSON.stringify({
                  username: invitee.username,
                  role: 'CLIENT',
                  clientId: clientId
                }), { headers: ownerHeaders });
                console.log(`Invite CLIENT ${invitee.username} to ${tenantId} -> status: ${inviteRes.status}`);

                const pendingRes = http.get(`${BASE_URL}/api/v1/workspaces/invitations`, { headers: inviteeHeaders });
                if (pendingRes.status === 200) {
                  const pendingInvites = JSON.parse(pendingRes.body);
                  const inviteToAccept = pendingInvites.find(inv => inv.workspaceId === workspaceUuid);
                  if (inviteToAccept) {
                    const acceptRes = http.post(`${BASE_URL}/api/v1/workspaces/invitations/${inviteToAccept.id}/accept`, '', { headers: inviteeHeaders });
                    console.log(`Accept CLIENT invite ${inviteToAccept.id} for ${invitee.username} -> status: ${acceptRes.status}`);
                  }
                }
              }
            } else {
              // Invite test_user_2..4 as MEMBER
              console.log(`User ${invitee.username} is not a member of ${tenantId}. Sending MEMBER invitation...`);
              const inviteRes = http.post(`${BASE_URL}/api/v1/workspaces/${tenantId}/invitations`, JSON.stringify({
                username: invitee.username,
                role: 'MEMBER'
              }), { headers: ownerHeaders });
              console.log(`Invite MEMBER ${invitee.username} to ${tenantId} -> status: ${inviteRes.status}`);

              const pendingRes = http.get(`${BASE_URL}/api/v1/workspaces/invitations`, { headers: inviteeHeaders });
              if (pendingRes.status === 200) {
                const pendingInvites = JSON.parse(pendingRes.body);
                const inviteToAccept = pendingInvites.find(inv => inv.workspaceId === workspaceUuid);
                if (inviteToAccept) {
                  const acceptRes = http.post(`${BASE_URL}/api/v1/workspaces/invitations/${inviteToAccept.id}/accept`, '', { headers: inviteeHeaders });
                  console.log(`Accept MEMBER invite ${inviteToAccept.id} for ${invitee.username} -> status: ${acceptRes.status}`);
                }
              }
            }
          } else {
            console.log(`User ${invitee.username} is already a member of ${tenantId}. Skipping invitation.`);
          }
        }
      }

      return {
        tenants: activeTenants,
        users: usersList
      };
    }
  }

  // 2. Fallback: Create 3 dynamic staging workspaces if database is empty and invite others
  console.log("No active workspaces found. Creating 3 dynamic staging workspaces...");
  const createdTenants = [];
  for (let i = 1; i <= 3; i++) {
    const res = http.post(`${BASE_URL}/api/v1/workspaces`, JSON.stringify({ name: `Staging Workspace ${i}` }), { headers: ownerHeaders });
    if (res.status === 201) {
      const workspace = JSON.parse(res.body);
      const tenantId = workspace.tenantId;
      createdTenants.push(tenantId);

      // Invite other users to each workspace and accept automatically
      for (let j = 1; j < usersList.length; j++) {
        const invitee = usersList[j];
        http.post(`${BASE_URL}/api/v1/workspaces/${tenantId}/invitations`, JSON.stringify({
          username: invitee.username,
          role: 'MEMBER'
        }), { headers: ownerHeaders });

        const inviteeHeaders = {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${invitee.token}`,
        };
        const pendingRes = http.get(`${BASE_URL}/api/v1/workspaces/invitations`, { headers: inviteeHeaders });
        if (pendingRes.status === 200) {
          const pendingInvites = JSON.parse(pendingRes.body);
          for (const invite of pendingInvites) {
            http.post(`${BASE_URL}/api/v1/workspaces/invitations/${invite.id}/accept`, null, { headers: inviteeHeaders });
          }
        }
      }
    } else {
      console.error(`Failed to create staging workspace ${i}: ${res.status} - ${res.body}`);
    }
  }

  if (createdTenants.length === 0) {
    throw new Error("Failed to initialize any staging workspaces!");
  }

  console.log(`Initialized staging workspaces: ${JSON.stringify(createdTenants)}`);
  return {
    tenants: createdTenants,
    users: usersList
  };
}

export default function (data) {
  const tenants = data.tenants;
  const users = data.users;

  if (!tenants || tenants.length === 0) {
    console.error("No active workspaces available for seeding!");
    return;
  }

  // Distribute seeding across the specified tenant list evenly, using the owner's token for seeding
  const tenantId = tenants[__ITER % tenants.length];
  const user = users[0]; // Owner seeds the database

  const headers = {
    'Content-Type': 'application/json',
    'X-Tenant-ID': tenantId,
    'Authorization': `Bearer ${user.token}`,
  };

  const clientName = `Globex Corp ${__ITER}`;
  
  // 1. Seed Client
  const clientPayload = JSON.stringify({
    name: clientName,
    email: `contact_${__ITER}@globex.com`,
    status: 'ACTIVE',
  });

  const clientRes = http.post(`${BASE_URL}/api/v1/clients`, clientPayload, { headers });

  check(clientRes, {
    'client created': (r) => r.status === 201,
  });

  if (clientRes.status !== 201) {
    console.error(`Failed to seed client: ${clientRes.status} - ${clientRes.body}`);
    return;
  }

  const client = JSON.parse(clientRes.body);
  const clientId = client.id;

  // 2. Seed Project for the Client
  const projectPayload = JSON.stringify({
    name: `Enterprise Website Redesign ${__ITER}`,
    description: `Redesigning corporate website with Next.js for Globex ${__ITER}`,
    budget: 15000.00,
    status: 'IN_PROGRESS',
    clientId: clientId,
    billingRate: 150.00,
  });

  const projectRes = http.post(`${BASE_URL}/api/v1/projects`, projectPayload, { headers });

  check(projectRes, {
    'project created': (r) => r.status === 201,
  });
}
