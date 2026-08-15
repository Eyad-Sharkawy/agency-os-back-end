# Performance and Stress Testing with k6

This directory contains performance testing scripts using [k6](https://k6.io/) to measure latency, throughput, and error rates of the Agency OS API under concurrent load.

## 1. Install k6

### Windows
Install using winget (recommended):
```bash
winget install grafana.k6
```
Or via Chocolatey:
```bash
choco install k6
```

### macOS
Install via Homebrew:
```bash
brew install k6
```

### Linux (Debian/Ubuntu)
```bash
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD19442217C0D68FFE49C6541D558619058F8D
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

## 2. Database Seeding (Recommended)

Before running the stress test, you should seed the database so queries perform actual index scans and data lookups.

Ensure you have your environment variables set (see Step 2 & 3 below for details) and run:

```bash
# PowerShell
$env:JWT_TOKEN="your_jwt_token_here"
$env:TENANT_ID="your_tenant_id_here"
$env:COUNT="50"  # Number of clients & projects to seed
k6 run seed-data.js

# Command Prompt
set JWT_TOKEN="your_jwt_token_here"
set TENANT_ID="your_tenant_id_here"
set COUNT="50"
k6 run seed-data.js

# Bash
JWT_TOKEN="your_jwt_token_here" TENANT_ID="your_tenant_id_here" COUNT=50 k6 run seed-data.js
```

## 3. Running Stress Tests via Wrapper Scripts (Recommended)

To simplify execution, use the wrapper scripts in this folder. They automatically load your `.env` variables and retrieve JWT access tokens programmatically from Keycloak on start:

```bash
# PowerShell (Windows)
./stress-tests/run-stress.ps1         # Runs both tests sequentially
./stress-tests/run-stress.ps1 workflow # Runs E2E workflow test only
./stress-tests/run-stress.ps1 project  # Runs read-stress test only
./stress-tests/run-stress.ps1 seed     # Runs database seeder

# Bash (Linux/macOS/Git Bash)
./stress-tests/run-stress.sh          # Runs both tests sequentially
./stress-tests/run-stress.sh workflow  # Runs E2E workflow test only
./stress-tests/run-stress.sh project   # Runs read-stress test only
./stress-tests/run-stress.sh seed      # Runs database seeder
```

---

## 4. Manual Execution (Alternative)

Because the API endpoints are secure, you need a valid Bearer JWT token from your Keycloak instance if you run them manually:

### Step 1: Run the Spring Boot App
Ensure your Spring Boot backend application is running locally:
```bash
# In back-end/
mvn spring-boot:run
```

### Step 2: Acquire a JWT Token
Extract the JWT access token from your frontend application browser console or curl Keycloak directly.

### Step 3: Choose Your Test Script

#### Option A: Basic Read Stress Test (`project-load-test.js`)
Measures read throughput of client projects in a single workspace.
```bash
# PowerShell
$env:JWT_TOKEN="your_jwt_token_here"
$env:TENANT_ID="your_tenant_id_here"
k6 run project-load-test.js

# Bash
JWT_TOKEN="your_jwt_token_here" TENANT_ID="your_tenant_id_here" k6 run project-load-test.js
```

#### Option B: Multi-Workspace Workflow Stress Test (`workflow-load-test.js`)
Tests real-world end-to-end scenarios (creating clients, creating projects, creating tasks, logging time, and listing resources) distributed across multiple dynamic tenant schemas concurrently.
```bash
# PowerShell (Optional: if TENANT_IDS is omitted, k6 automatically creates/discovers workspaces!)
$env:JWT_TOKEN="your_jwt_token_here"
$env:TENANT_IDS="workspace_tenant_a,workspace_tenant_b,workspace_tenant_c"
k6 run workflow-load-test.js

# Bash
JWT_TOKEN="your_jwt_token_here" TENANT_IDS="tenant_a,tenant_b,tenant_c" k6 run workflow-load-test.js
```

---

## 5. Customizing the Load Profile

You can modify the load parameters in [`project-load-test.js`](file:///c:/Users/eyadd/Documents/Java/agency-os/back-end/stress-tests/project-load-test.js) options:

```javascript
export const options = {
  stages: [
    { duration: '1m', target: 50 },  // Ramp up to 50 concurrent users
    { duration: '5m', target: 50 },  // Maintain stress load
    { duration: '30s', target: 0 },  // Ramp down to 0
  ],
  thresholds: {
    // Assert 95% of requests take < 250ms
    http_req_duration: ['p(95)<250'],
    // Assert less than 0.5% request failure rate
    http_req_failed: ['rate<0.005'],
  },
};
```

---

## 6. Understanding Results

When the test runs, k6 outputs details in your console:
*   `http_req_duration`: Measures latencies (`p90`, `p95`, `p99` are the 90th, 95th, and 99th percentiles).
*   `http_reqs`: Shows total requests processed and throughput (requests/sec).
*   `http_req_failed`: Track error response rates. If this spikes, your DB connection pool or CPU is bottlenecked.
