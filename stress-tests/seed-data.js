import http from 'k6/http';
import { check } from 'k6';

// Single Virtual User running sequentially to seed structured records
export const options = {
  vus: 1,
  iterations: __ENV.COUNT ? parseInt(__ENV.COUNT) : 50, // Default to 50 records
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080';
const AUTH_TOKEN = __ENV.JWT_TOKEN || '';

export function setup() {
  const TENANTS_ENV = __ENV.TENANT_IDS || __ENV.TENANT_ID || '';
  if (TENANTS_ENV) {
    return { tenants: TENANTS_ENV.split(',').map(s => s.trim()) };
  }

  if (!AUTH_TOKEN) {
    console.error("JWT_TOKEN is required to run the seeder script!");
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

  // 2. Fallback: Create 3 dynamic staging workspaces if database is empty
  console.log("No active workspaces found. Creating 3 dynamic staging workspaces...");
  const createdTenants = [];
  for (let i = 1; i <= 3; i++) {
    const res = http.post(`${BASE_URL}/api/v1/workspaces`, JSON.stringify({ name: `Staging Workspace ${i}` }), { headers });
    if (res.status === 201) {
      const workspace = JSON.parse(res.body);
      createdTenants.push(workspace.tenantId);
    } else {
      console.error(`Failed to create staging workspace ${i}: ${res.status} - ${res.body}`);
    }
  }

  if (createdTenants.length === 0) {
    throw new Error("Failed to initialize any staging workspaces!");
  }

  console.log(`Initialized staging workspaces: ${JSON.stringify(createdTenants)}`);
  return { tenants: createdTenants };
}

export default function (data) {
  if (!AUTH_TOKEN) {
    console.error("JWT_TOKEN is required to run the seeder script!");
    return;
  }

  const tenants = data.tenants;
  if (!tenants || tenants.length === 0) {
    console.error("No active workspaces available for seeding!");
    return;
  }

  // Distribute seeding across the specified tenant list evenly
  const tenantId = tenants[__ITER % tenants.length];

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
