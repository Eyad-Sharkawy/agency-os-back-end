#!/bin/bash

TYPE=${1:-all}

if [[ "$TYPE" != "project" && "$TYPE" != "workflow" && "$TYPE" != "all" && "$TYPE" != "seed" ]]; then
  echo "Usage: ./run-stress.sh [project|workflow|all|seed]"
  exit 1
fi

# Clear cached environment variables to prevent leakage from previous runs in the active shell session
unset TENANT_IDS
unset TENANT_ID
unset JWT_TOKEN
unset JWT_TOKENS

# 1. Load .env file from root directory
ENV_FILE="../.env"
HAS_MANUAL_TOKEN=false
if [ -f "$ENV_FILE" ]; then
  while IFS= read -r line || [ -n "$line" ]; do
    # Skip comments and empty lines
    if [[ ! "$line" =~ ^# ]] && [[ "$line" =~ = ]]; then
      name=$(echo "$line" | cut -d'=' -f1 | xargs)
      value=$(echo "$line" | cut -d'=' -f2- | xargs | sed -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")
      export "$name=$value"
      if [[ "$name" == "JWT_TOKEN" && -n "$value" ]]; then
        HAS_MANUAL_TOKEN=true
      fi
    fi
  done < "$ENV_FILE"
else
  echo "Warning: .env file not found at $ENV_FILE"
fi

# 2. Get JWT Tokens from Keycloak programmatically if not manually set in .env
CLIENT_ID="${KEYCLOAK_TEST_CLIENT_ID:-$KEYCLOAK_FRONTEND_CLIENT_ID}"
USERNAMES_STR="${TEST_USER_USERNAMES:-$TEST_USER_USERNAME}"

if [ "$HAS_MANUAL_TOKEN" = false ] && [ -n "$KEYCLOAK_ISSUER_URI" ] && [ -n "$CLIENT_ID" ] && [ -n "$USERNAMES_STR" ] && [ -n "$TEST_USER_PASSWORD" ]; then
  echo "Fetching JWT Tokens from Keycloak for users: $USERNAMES_STR..."
  SECRET_PARAM=""
  if [ -n "$KEYCLOAK_TEST_CLIENT_SECRET" ]; then
    SECRET_PARAM="-d client_secret=${KEYCLOAK_TEST_CLIENT_SECRET}"
  fi

  # Split usernames by comma
  IFS=',' read -r -a usernames_array <<< "$USERNAMES_STR"
  TOKENS_LIST=""
  for username in "${usernames_array[@]}"; do
    username_trimmed=$(echo "$username" | xargs)
    if [ -z "$username_trimmed" ]; then
      continue
    fi
    TOKEN_RES=$(curl -s -X POST "${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/token" \
      -H "Content-Type: application/x-www-form-urlencoded" \
      -d "grant_type=password" \
      -d "client_id=${CLIENT_ID}" \
      -d "username=${username_trimmed}" \
      -d "password=${TEST_USER_PASSWORD}" \
      $SECRET_PARAM)
    
    SINGLE_TOKEN=$(echo "$TOKEN_RES" | grep -o '"access_token":"[^"]*' | grep -o '[^"]*$')
    if [ -n "$SINGLE_TOKEN" ]; then
      if [ -z "$TOKENS_LIST" ]; then
        TOKENS_LIST="$SINGLE_TOKEN"
      else
        TOKENS_LIST="${TOKENS_LIST},${SINGLE_TOKEN}"
      fi
    else
      echo "Error: Failed to fetch token for user $username_trimmed. Response was: $TOKEN_RES"
    fi
  done
  
  if [ -n "$TOKENS_LIST" ]; then
    export JWT_TOKENS="$TOKENS_LIST"
    export JWT_TOKEN=$(echo "$TOKENS_LIST" | cut -d',' -f1) # Fallback for backward compatibility
    echo "Successfully retrieved JWT Tokens."
  fi
fi

# 3. Resolve URL defaults
API_URL="${API_URL:-http://localhost:8080}"

# 4. Run scripts
if [[ "$TYPE" == "seed" ]]; then
  echo "Running database seeder [seed] against URL [$API_URL]..."
  k6 run "$(dirname "$0")/seed-data.js"
else
  if [[ "$TYPE" == "all" || "$TYPE" == "project" ]]; then
    echo "Running stress test of type [project] against URL [$API_URL]..."
    k6 run "$(dirname "$0")/project-load-test.js"
  fi

  if [[ "$TYPE" == "all" || "$TYPE" == "workflow" ]]; then
    echo "Running stress test of type [workflow] against URL [$API_URL]..."
    k6 run "$(dirname "$0")/workflow-load-test.js"
  fi
fi
