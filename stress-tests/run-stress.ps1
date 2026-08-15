param (
    [Parameter(Mandatory=$false)]
    [ValidateSet("project", "workflow", "all", "seed")]
    [string]$Type = "all"
)

$hasManualToken = $false
# 1. Load .env file from the root directory
$envFile = Join-Path $PSScriptRoot "../.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
            $name, $value = $line.Split('=', 2)
            $name = $name.Trim()
            $value = $value.Trim().Trim('"').Trim("'")
            [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
            if ($name -eq "JWT_TOKEN" -and $value) {
                $hasManualToken = $true
            }
        }
    }
} else {
    Write-Warning "Could not find .env file at $envFile"
}

# 2. Get JWT Token from Keycloak programmatically if not manually set in .env
$clientId = if ($env:KEYCLOAK_TEST_CLIENT_ID) { $env:KEYCLOAK_TEST_CLIENT_ID } else { $env:KEYCLOAK_FRONTEND_CLIENT_ID }

if (-not $hasManualToken -and $env:KEYCLOAK_ISSUER_URI -and $clientId -and $env:TEST_USER_USERNAME -and $env:TEST_USER_PASSWORD) {
    Write-Host "Fetching JWT Token from Keycloak..." -ForegroundColor Cyan
    try {
        $tokenUrl = "$($env:KEYCLOAK_ISSUER_URI)/protocol/openid-connect/token"
        $body = @{
            grant_type = "password"
            client_id  = $clientId
            username   = $env:TEST_USER_USERNAME
            password   = $env:TEST_USER_PASSWORD
        }
        if ($env:KEYCLOAK_TEST_CLIENT_SECRET) {
            $body.Add("client_secret", $env:KEYCLOAK_TEST_CLIENT_SECRET)
        }
        $response = Invoke-RestMethod -Uri $tokenUrl -Method Post -ContentType "application/x-www-form-urlencoded" -Body $body
        $env:JWT_TOKEN = $response.access_token
        Write-Host "Successfully retrieved JWT Token." -ForegroundColor Green
    } catch {
        Write-Error "Failed to fetch JWT Token from Keycloak: $_"
    }
}

# 3. Resolve URL defaults
$apiUrl = if ($env:API_URL) { $env:API_URL } else { "http://localhost:8080" }

# 4. Execute scripts
if ($Type -eq "seed") {
    $scriptPath = Join-Path $PSScriptRoot "seed-data.js"
    Write-Host "Running database seeder [seed] against URL [$apiUrl]..." -ForegroundColor Green
    k6 run $scriptPath
} else {
    if ($Type -eq "all" -or $Type -eq "project") {
        $scriptPath = Join-Path $PSScriptRoot "project-load-test.js"
        Write-Host "Running stress test of type [project] against URL [$apiUrl]..." -ForegroundColor Green
        k6 run $scriptPath
    }

    if ($Type -eq "all" -or $Type -eq "workflow") {
        $scriptPath = Join-Path $PSScriptRoot "workflow-load-test.js"
        Write-Host "Running stress test of type [workflow] against URL [$apiUrl]..." -ForegroundColor Green
        k6 run $scriptPath
    }
}
