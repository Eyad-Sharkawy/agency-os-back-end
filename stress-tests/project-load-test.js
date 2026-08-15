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

// Parse workspace tenant: use TENANT_ID or the first element from TENANT_IDS list
const TENANT_ID_ENV = __ENV.TENANT_ID || __ENV.TENANT_IDS || 'test_workspace_tenant';
const TENANT_ID = TENANT_ID_ENV.split(',')[0].trim();

export default function () {
  // Setup headers required for the multi-tenant secure endpoint
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-ID': TENANT_ID,
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
