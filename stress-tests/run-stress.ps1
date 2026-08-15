param (
    [Parameter(Mandatory=$false)]
    [ValidateSet("project", "workflow", "all", "seed")]
    [string]$Type = "all"
)

# Clear cached environment variables to prevent leakage from previous runs in the active shell session
[System.Environment]::SetEnvironmentVariable("TENANT_IDS", $null, "Process")
[System.Environment]::SetEnvironmentVariable("TENANT_ID", $null, "Process")
[System.Environment]::SetEnvironmentVariable("JWT_TOKEN", $null, "Process")
[System.Environment]::SetEnvironmentVariable("JWT_TOKENS", $null, "Process")

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

# 2. Get JWT Tokens from Keycloak programmatically if not manually set in .env
$clientId = if ($env:KEYCLOAK_TEST_CLIENT_ID) { $env:KEYCLOAK_TEST_CLIENT_ID } else { $env:KEYCLOAK_FRONTEND_CLIENT_ID }
$usernames = if ($env:TEST_USER_USERNAMES) { $env:TEST_USER_USERNAMES.Split(',') } else { @($env:TEST_USER_USERNAME) }

if (-not $hasManualToken -and $env:KEYCLOAK_ISSUER_URI -and $clientId -and $usernames.Count -gt 0 -and $env:TEST_USER_PASSWORD) {
    Write-Host "Fetching JWT Tokens from Keycloak for users: $($env:TEST_USER_USERNAMES)..." -ForegroundColor Cyan
    try {
        $tokenUrl = "$($env:KEYCLOAK_ISSUER_URI)/protocol/openid-connect/token"
        $tokenList = @()
        foreach ($username in $usernames) {
            $username = $username.Trim()
            if (-not $username) { continue }
            $body = @{
                grant_type = "password"
                client_id  = $clientId
                username   = $username
                password   = $env:TEST_USER_PASSWORD
            }
            if ($env:KEYCLOAK_TEST_CLIENT_SECRET) {
                $body.Add("client_secret", $env:KEYCLOAK_TEST_CLIENT_SECRET)
            }
            $response = Invoke-RestMethod -Uri $tokenUrl -Method Post -ContentType "application/x-www-form-urlencoded" -Body $body
            $tokenList += $response.access_token
        }
        $env:JWT_TOKENS = $tokenList -join ","
        $env:JWT_TOKEN = $tokenList[0] # Fallback for backward compatibility
        Write-Host "Successfully retrieved $($tokenList.Count) JWT Tokens." -ForegroundColor Green
    } catch {
        Write-Error "Failed to fetch JWT Tokens from Keycloak: $_"
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
