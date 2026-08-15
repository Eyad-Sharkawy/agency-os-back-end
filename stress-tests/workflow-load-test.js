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

// Parse comma-separated list of active workspaces/tenants (e.g. TENANT_IDS="tenant1,tenant2")
const TENANTS_ENV = __ENV.TENANT_IDS || __ENV.TENANT_ID || 'test_workspace_tenant';
const TENANTS = TENANTS_ENV.split(',').map(s => s.trim());

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

const KEYCLOAK_USER_ID = getUserIdFromToken(AUTH_TOKEN);

export default function () {
  if (!AUTH_TOKEN) {
    console.error("JWT_TOKEN is required to run the workflow stress test!");
    return;
  }

  // Assign this VU to a specific tenant randomly (to test multi-schema routing concurrency)
  const tenantId = TENANTS[__VU % TENANTS.length];
  
  const headers = {
    'Content-Type': 'application/json',
    'X-Tenant-ID': tenantId,
    'Authorization': `Bearer ${AUTH_TOKEN}`,
  };

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
  
  if (!clientOk) return;
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
    assigneeIds: [KEYCLOAK_USER_ID],
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
