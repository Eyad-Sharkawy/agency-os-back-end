pipeline {
    agent {
        label 'oracle-host-agent'
    }

    environment {
        DB_URL = 'jdbc:postgresql://localhost:5433/agency_os'
        DB_USERNAME = 'postgres'
        DB_PASSWORD = 'password'
        KEYCLOAK_ISSUER_URI = 'https://key-cloak.duckdns.org/realms/agency-os-realm'
    }

    stages {
        stage('Build and Test') {
            when {
                anyOf {
                    changeset 'src/**/*'
                    changeset 'pom.xml'
                    changeset 'Dockerfile'
                    changeset 'docker-compose.yml'
                    changeset 'Jenkinsfile'
                    changeset 'checkstyle.xml'
                    changeset '.dockerignore'
                    changeset '.mvn/**/*'
                    changeset 'mvnw'
                    changeset 'mvnw.cmd'
                    changeset '.env'
                    changeset '.env.example'
                }
            }
            stages {
                stage('Quality Checks') {
                    steps {
                        sh 'chmod +x ./mvnw'
                        sh './mvnw spotless:check'
                        sh './mvnw checkstyle:check'
                    }
                }

                stage('Integration Tests') {
                    steps {
                        sh 'docker rm -f pg-test || true'

                        sh 'docker run --rm --name pg-test -e POSTGRES_DB=agency_os -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password -p 5433:5432 -d postgres:15-alpine'

                        sh '''
                            echo "Waiting for PostgreSQL to be ready..."
                            until docker exec pg-test pg_isready -U postgres > /dev/null 2>&1; do
                                sleep 1
                            done
                            echo "PostgreSQL is ready!"
                        '''

                        script {
                            try {
                                sh './mvnw verify'
                            } finally {
                                sh 'docker stop pg-test || true'
                            }
                        }
                    }
                }

                stage('SonarQube Analysis') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            sh './mvnw sonar:sonar'
                        }
                        
                        timeout(time: 5, unit: 'MINUTES') {
                            waitForQualityGate abortPipeline: true
                        }
                    }
                }

                stage('Performance Verification') {
                    when {
                        branch 'main'
                    }
                    environment {
                        TEST_USER_CREDS = credentials('agency-os-keycloak-test-user')
                        KEYCLOAK_CLIENT_SECRET = credentials('agency-os-keycloak-test-client-secret')
                        KEYCLOAK_CLIENT_ID = 'agency-os-test'
                    }
                    steps {
                        sh '''
                            # Remove any leftover containers from aborted runs
                            docker rm -f agency-os-staging pg-perf-test || true

                            # 1. Start a temporary staging database container
                            echo "Starting staging database container..."
                            docker run -d --name pg-perf-test \
                              --network agency-os-net \
                              -e POSTGRES_DB=agency_os \
                              -e POSTGRES_USER=postgres \
                              -e POSTGRES_PASSWORD=password \
                              postgres:15-alpine

                            # 2. Wait for staging database to be ready
                            echo "Waiting for staging database to start..."
                            until docker exec pg-perf-test pg_isready -U postgres > /dev/null 2>&1; do
                                sleep 1
                            done
                            echo "Staging database is ready!"

                            # 3. Build staging backend container image
                            echo "Building staging backend container..."
                            docker build -t agency-os-staging:latest .

                            # 4. Start staging backend container
                            echo "Starting staging backend container..."
                            docker run -d --name agency-os-staging \
                              --network agency-os-net \
                              -e SPRING_DATASOURCE_URL="jdbc:postgresql://pg-perf-test:5432/agency_os" \
                              -e SPRING_DATASOURCE_USERNAME="postgres" \
                              -e SPRING_DATASOURCE_PASSWORD="password" \
                              -e KEYCLOAK_ISSUER_URI="${KEYCLOAK_ISSUER_URI}" \
                              agency-os-staging:latest

                            # 5. Wait for the staging backend container to be fully initialized and healthy
                            echo "Waiting for staging backend container to start..."
                            docker run --rm \
                              --network agency-os-net \
                              curlimages/curl -s --retry 15 --retry-delay 2 --retry-connrefused http://agency-os-staging:8080/api/v1/workspaces > /dev/null || true
                            echo "Staging backend is ready! Starting stress tests..."

                            # 6. Fetch JWT tokens programmatically from Keycloak for all 5 users
                            echo "Fetching tokens for test users..."
                            JWT_TOKENS=""
                            for i in "" "_2" "_3" "_4" "_5"; do
                                USERNAME="${TEST_USER_CREDS_USR}${i}"
                                TOKEN_RES=$(curl -s -X POST "${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/token" \
                                  -H "Content-Type: application/x-www-form-urlencoded" \
                                  -d "grant_type=password" \
                                  -d "client_id=${KEYCLOAK_CLIENT_ID}" \
                                  -d "client_secret=${KEYCLOAK_CLIENT_SECRET}" \
                                  -d "username=${USERNAME}" \
                                  -d "password=${TEST_USER_CREDS_PSW}")
                                
                                SINGLE_TOKEN=$(echo "$TOKEN_RES" | grep -o '"access_token":"[^"]*' | grep -o '[^"]*$')
                                if [ -n "$SINGLE_TOKEN" ]; then
                                    if [ -z "$JWT_TOKENS" ]; then
                                        JWT_TOKENS="$SINGLE_TOKEN"
                                    else
                                        JWT_TOKENS="${JWT_TOKENS},${SINGLE_TOKEN}"
                                    fi
                                else
                                    echo "Error: Failed to fetch token for user ${USERNAME}"
                                    exit 1
                                fi
                            done
                            echo "Retrieved tokens successfully."
 
                            # 7. Run k6 database seeder (will dynamically create workspaces and seed data!)
                            echo "Seeding the empty staging database..."
                            docker run --rm \
                              --network agency-os-net \
                              -e API_URL="http://agency-os-staging:8080" \
                              -e JWT_TOKENS="$JWT_TOKENS" \
                              -e TENANT_IDS="" \
                              -e TENANT_ID="" \
                              -v "$(pwd)/stress-tests:/stress-tests" \
                              grafana/k6 run /stress-tests/seed-data.js

                            # 8. Run k6 read-stress test (project-load-test.js)
                            echo "Running project read stress test..."
                            docker run --rm \
                              --network agency-os-net \
                              -e API_URL="http://agency-os-staging:8080" \
                              -e JWT_TOKENS="$JWT_TOKENS" \
                              -e TENANT_IDS="" \
                              -e TENANT_ID="" \
                              -v "$(pwd)/stress-tests:/stress-tests" \
                              grafana/k6 run /stress-tests/project-load-test.js

                            # 9. Run k6 E2E workflow stress test (workflow-load-test.js)
                            echo "Running E2E workflow stress test..."
                            docker run --rm \
                              --network agency-os-net \
                              -e API_URL="http://agency-os-staging:8080" \
                              -e JWT_TOKENS="$JWT_TOKENS" \
                              -e TENANT_IDS="" \
                              -e TENANT_ID="" \
                              -v "$(pwd)/stress-tests:/stress-tests" \
                              grafana/k6 run /stress-tests/workflow-load-test.js
                        '''
                    }
                    post {
                        always {
                            sh '''
                                echo "Cleaning up staging containers..."
                                docker rm -f agency-os-staging pg-perf-test || true
                            '''
                        }
                    }
                }

                stage('Deploy to Server') {
                    when {
                        branch 'main'
                    }
                    steps {
                        sh '''
                            rsync -av --exclude='.git' --exclude='target' ./ /home/ubuntu/agency-os-back-end/

                            cd /home/ubuntu/agency-os-back-end
                            docker compose down
                            docker compose up -d --build
                        '''
                    }
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'target/*-reports/*.xml'
        }
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed. Check the logs.'

            mail to: 'eyad.m.sharkawy@gmail.com',
                 subject: "FAILED: Job '${env.JOB_NAME}' [Build #${env.BUILD_NUMBER}]",
                 body: "Your Jenkins pipeline failed. Check the logs at ${env.BUILD_URL}"
        }
    }
}
