import http from 'k6/http';
import { check, sleep } from 'k6';
import encoding from 'k6/encoding';

// k6 options to configure load profile
export const options = {
  stages: [
    { duration: '15s', target: 8 },  // Ramp-up: 0 to 8 users
    { duration: '30s', target: 8 },  // Stress: hold 8 users
    { duration: '15s', target: 0 },  // Ramp-down: 8 to 0 users
  ],
  thresholds: {
    // 95% of requests must complete in less than 400ms
    http_req_duration: ['p(95)<400'],
    // Error rate must be less than 1%
    http_req_failed: ['rate<0.01'],
  },
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

  const BASE_URL = __ENV.API_URL || 'http://localhost:8080';

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

  // 1. Fetch user's existing workspaces (populated by the seeder) using owner's token
  const ownerHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${usersList[0].token}`,
  };

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

  // 2. Fallback: Create a dynamic staging workspace if database is empty and invite others
  console.log("No active workspaces found. Creating a dynamic staging workspace...");
  const createdTenants = [];
  const createRes = http.post(`${BASE_URL}/api/v1/workspaces`, JSON.stringify({ name: 'Staging Workspace' }), { headers: ownerHeaders });
  if (createRes.status === 201) {
    const workspace = JSON.parse(createRes.body);
    const tenantId = workspace.tenantId;
    createdTenants.push(tenantId);
    console.log(`Created staging workspace: ${tenantId}. Inviting teammates...`);

    // Invite the other 4 test users and automatically accept
    for (let i = 1; i < usersList.length; i++) {
      const invitee = usersList[i];
      const inviteRes = http.post(`${BASE_URL}/api/v1/workspaces/${tenantId}/invitations`, JSON.stringify({
        username: invitee.username,
        role: 'MEMBER'
      }), { headers: ownerHeaders });
      console.log(`Fallback Invite ${invitee.username} to ${tenantId} -> status: ${inviteRes.status}, body: ${inviteRes.body}`);

      const inviteeHeaders = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${invitee.token}`,
      };
      const pendingRes = http.get(`${BASE_URL}/api/v1/workspaces/invitations`, { headers: inviteeHeaders });
      console.log(`Fallback Get pending invites for ${invitee.username} -> status: ${pendingRes.status}, body: ${pendingRes.body}`);
      if (pendingRes.status === 200) {
        const pendingInvites = JSON.parse(pendingRes.body);
        for (const invite of pendingInvites) {
          const acceptRes = http.post(`${BASE_URL}/api/v1/workspaces/invitations/${invite.id}/accept`, '', { headers: inviteeHeaders });
          console.log(`Fallback Accept invite ${invite.id} for ${invitee.username} -> status: ${acceptRes.status}, body: ${acceptRes.body}`);
        }
      }
    }

    return {
      tenants: createdTenants,
      users: usersList
    };
  }

  throw new Error("Failed to resolve or initialize any staging workspaces!");
}

export default function (data) {
  const tenants = data.tenants;
  const users = data.users;

  if (!tenants || tenants.length === 0) {
    console.error("No active workspaces available for read stress test!");
    return;
  }

  // Distribute users and workspaces across the virtual users
  const user = users[__VU % users.length];
  const tenantId = tenants[__VU % tenants.length];

  // Setup headers required for the multi-tenant secure endpoint
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-ID': tenantId,
      'Authorization': `Bearer ${user.token}`,
    },
  };

  // Hit the project listing endpoint (secured by TenantSecurityFilter and @PreAuthorize)
  const res = http.get(`${BASE_URL}/api/v1/projects`, params);

  // Validate response status
  const success = check(res, {
    'status is 200': (r) => r.status === 200,
    'body is not empty': (r) => r.body && r.body.length > 0,
  });

  if (!success) {
    throw new Error(`Read failed for ${user.username} on workspace ${tenantId} -> status: ${res.status}, body: ${res.body}`);
  }

  // Wait 200ms between iterations for each virtual user to control request throughput
  sleep(0.2);
}
