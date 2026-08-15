import http from 'k6/http';
import { check, sleep } from 'k6';

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

// Retrieve environment variables or use default fallbacks
const BASE_URL = __ENV.API_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.JWT_TOKEN || '';

export function setup() {
  const TENANTS_ENV = __ENV.TENANT_IDS || __ENV.TENANT_ID || '';
  if (TENANTS_ENV) {
    return { tenants: TENANTS_ENV.split(',').map(s => s.trim()) };
  }

  if (!AUTH_TOKEN) {
    console.error("JWT_TOKEN is required to run the read stress test!");
    return { tenants: [] };
  }

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${AUTH_TOKEN}`,
  };

  // 1. Fetch user's existing workspaces (populated by the seeder)
  console.log("No TENANT_IDS provided. Querying active user workspaces...");
  const listRes = http.get(`${BASE_URL}/api/v1/workspaces`, { headers });
  if (listRes.status === 200) {
    const workspaces = JSON.parse(listRes.body);
    if (workspaces.length > 0) {
      const activeTenants = workspaces.map(w => w.tenantId);
      console.log(`Found active workspaces: ${JSON.stringify(activeTenants)}`);
      return { tenants: activeTenants };
    }
  }

  // 2. Fallback: Create a dynamic staging workspace if database is empty
  console.log("No active workspaces found. Creating a dynamic staging workspace...");
  const createRes = http.post(`${BASE_URL}/api/v1/workspaces`, JSON.stringify({ name: 'Staging Workspace' }), { headers });
  if (createRes.status === 201) {
    const workspace = JSON.parse(createRes.body);
    console.log(`Created staging workspace: ${workspace.tenantId}`);
    return { tenants: [workspace.tenantId] };
  }

  throw new Error("Failed to resolve or initialize any staging workspaces!");
}

export default function (data) {
  const tenants = data.tenants;
  if (!tenants || tenants.length === 0) {
    console.error("No active workspaces available for read stress test!");
    return;
  }

  const tenantId = tenants[__VU % tenants.length];

  // Setup headers required for the multi-tenant secure endpoint
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-ID': tenantId,
      'Authorization': AUTH_TOKEN ? `Bearer ${AUTH_TOKEN}` : '',
    },
  };

  // Hit the project listing endpoint (secured by TenantSecurityFilter and @PreAuthorize)
  const res = http.get(`${BASE_URL}/api/v1/projects`, params);

  // Validate response status
  check(res, {
    'status is 200': (r) => r.status === 200,
    'body is not empty': (r) => r.body && r.body.length > 0,
  });

  // Wait 200ms between iterations for each virtual user to control request throughput
  sleep(0.2);
}
