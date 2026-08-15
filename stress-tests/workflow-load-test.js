import http from 'k6/http';
import { check, sleep } from 'k6';
import encoding from 'k6/encoding';

export const options = {
  stages: [
    { duration: '15s', target: 8 },  // Ramp up to 8 concurrent VUs
    { duration: '30s', target: 8 },  // Hold 8 VUs under stress
    { duration: '15s', target: 0 },  // Ramp down to 0
  ],
  thresholds: {
    // 95% of request actions should take under 950ms for write-heavy flows
    http_req_duration: ['p(95)<950'],
    // Error rate must be less than 2%
    http_req_failed: ['rate<0.02'],
  },
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.JWT_TOKEN || '';

// Helper function to decode Keycloak User ID (sub) from JWT
function getUserIdFromToken(token) {
  try {
    const parts = token.split('.');
    if (parts.length < 2) return 'user-stress-test';
    // Base64URL decode the JWT payload
    const decoded = encoding.b64decode(parts[1], 'rawurl', 's');
    const payload = JSON.parse(decoded);
    return payload.sub || 'user-stress-test';
  } catch (e) {
    console.error(`Failed to parse JWT: ${e}`);
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
  console.log("Registering and retrieving owner workspaces for all 5 test users...");
  const registrationWorkspaces = [];
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
      if (regRes.status === 201 || regRes.status === 200) {
        const workspace = JSON.parse(regRes.body);
        tenantId = workspace.tenantId;
      }
    }

    registrationWorkspaces.push(tenantId);
  }

  const TENANTS_ENV = __ENV.TENANT_IDS || __ENV.TENANT_ID || '';
  if (TENANTS_ENV) {
    return {
      tenants: TENANTS_ENV.split(',').map(s => s.trim()),
      users: usersList
    };
  }

  // If no TENANT_IDS provided, use the registration workspaces of each user
  if (registrationWorkspaces.length > 0 && registrationWorkspaces.every(t => t !== '')) {
    console.log(`Using owner workspaces for write test: ${JSON.stringify(registrationWorkspaces)}`);
    return {
      tenants: registrationWorkspaces,
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
        for (let i = 1; i < usersList.length; i++) {
          const invitee = usersList[i];
          
          // 1. Send invitation (ignored if already invited/member)
          http.post(`${BASE_URL}/api/v1/workspaces/${tenantId}/invitations`, JSON.stringify({
            username: invitee.username,
            role: 'MEMBER'
          }), { headers: ownerHeaders });

          // 2. Fetch and accept all pending invitations for this user
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
    console.error("No active workspaces available for workflow stress test!");
    return;
  }

  // Distribute users and workspaces across the virtual users
  const user = users[__VU % users.length];
  const tenantId = tenants[__VU % tenants.length];
  
  const headers = {
    'Content-Type': 'application/json',
    'X-Tenant-ID': tenantId,
    'Authorization': `Bearer ${user.token}`,
  };

  // Decode the current user's ID for correct assignment inside operations
  const currentUserId = user.userId;
  const uniqueId = `${__VU}_${__ITER}`;

  // 1. Create a Client (Write)
  const clientPayload = JSON.stringify({
    name: `Client ${uniqueId}`,
    email: `client_${uniqueId}@stress.com`,
    status: 'ACTIVE',
  });
  const clientRes = http.post(`${BASE_URL}/api/v1/clients`, clientPayload, { headers });
  
  const clientOk = check(clientRes, {
    'client created (201)': (r) => r.status === 201,
  });
  
  if (!clientOk) {
    throw new Error(`Client creation failed for ${user.username} on workspace ${tenantId} -> status: ${clientRes.status}, body: ${clientRes.body}`);
  }
  const client = JSON.parse(clientRes.body);

  // 2. Create a Project linked to the Client (Write)
  const projectPayload = JSON.stringify({
    name: `Project ${uniqueId}`,
    description: `Stress testing project under ${tenantId}`,
    budget: 50000.00,
    status: 'IN_PROGRESS',
    clientId: client.id,
    billingRate: 120.00,
  });
  const projectRes = http.post(`${BASE_URL}/api/v1/projects`, projectPayload, { headers });

  const projectOk = check(projectRes, {
    'project created (201)': (r) => r.status === 201,
  });

  if (!projectOk) return;
  const project = JSON.parse(projectRes.body);

  // 3. Create a Task linked to the Project (Write)
  const taskPayload = JSON.stringify({
    title: `Task ${uniqueId}`,
    description: `Workflow execution task for project ${project.id}`,
    startDate: new Date().toISOString(),
    dueDate: new Date(Date.now() + 864000000).toISOString(), // 10 days out
    estimatedMinutes: 240,
    priority: 'HIGH',
    status: 'TODO',
    projectId: project.id,
    assigneeIds: [currentUserId],
  });
  const taskRes = http.post(`${BASE_URL}/api/v1/tasks`, taskPayload, { headers });

  const taskOk = check(taskRes, {
    'task created (201)': (r) => r.status === 201,
  });

  if (!taskOk) return;
  const task = JSON.parse(taskRes.body);

  // 4. Log a Time Entry on the Task (Write)
  const timePayload = JSON.stringify({
    taskId: task.id,
    durationMinutes: 120,
    isBillable: true,
  });
  const timeRes = http.post(`${BASE_URL}/api/v1/time-entries`, timePayload, { headers });
  check(timeRes, {
    'time entry logged (201)': (r) => r.status === 201,
  });

  // 5. Fetch Projects List (Read)
  const listProjectsRes = http.get(`${BASE_URL}/api/v1/projects`, { headers });
  check(listProjectsRes, {
    'get projects succeeded (200)': (r) => r.status === 200,
  });

  // 6. Fetch Tasks List (Read)
  const listTasksRes = http.get(`${BASE_URL}/api/v1/tasks`, { headers });
  check(listTasksRes, {
    'get tasks succeeded (200)': (r) => r.status === 200,
  });

  // Control pacing to mimic human user response cycles (approx 300ms sleep)
  sleep(0.3);
}
