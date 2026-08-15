import http from 'k6/http';
import { check } from 'k6';

// Single Virtual User running sequentially to seed structured records
export const options = {
  vus: 1,
  iterations: __ENV.COUNT ? parseInt(__ENV.COUNT) : 50, // Default to 50 records
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.JWT_TOKEN || '';

// Parse comma-separated list of active workspaces/tenants (e.g. TENANT_IDS="tenant1,tenant2")
const TENANTS_ENV = __ENV.TENANT_IDS || __ENV.TENANT_ID || 'test_workspace_tenant';
const TENANTS = TENANTS_ENV.split(',').map(s => s.trim());

export default function (data) {
  if (!AUTH_TOKEN) {
    console.error("JWT_TOKEN is required to run the seeder script!");
    return;
  }

  // Distribute seeding across the specified tenant list evenly
  const tenantId = TENANTS[__ITER % TENANTS.length];

  const headers = {
    'Content-Type': 'application/json',
    'X-Tenant-ID': tenantId,
    'Authorization': `Bearer ${AUTH_TOKEN}`,
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
