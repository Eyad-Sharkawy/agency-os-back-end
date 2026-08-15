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

                stage('Performance Verification') {
                    when {
                        branch 'main'
                    }
                    environment {
                        TEST_USER_CREDS = credentials('agency-os-keycloak-test-user')
                        KEYCLOAK_CLIENT_SECRET = credentials('agency-os-keycloak-test-client-secret')
                        KEYCLOAK_CLIENT_ID = 'agency-os-test'
                        TEST_TENANTS = 'tenant_test_1_02f836,tenant_test_2_3596a7,tenant_test_3_b5aac1'
                    }
                    steps {
                        sh '''
                            # 1. Wait for the backend container to be fully initialized and healthy
                            echo "Waiting for backend container to start..."
                            docker run --rm \
                              --network agency-os-net \
                              curlimages/curl -s --retry 15 --retry-delay 2 --retry-connrefused http://agency-os:8080/api/v1/workspaces > /dev/null || true
                            echo "Backend is ready! Starting stress tests..."

                            # 2. Fetch JWT token programmatically from Keycloak via credentials
                            TOKEN_RES=$(curl -s -X POST "${KEYCLOAK_ISSUER_URI}/protocol/openid-connect/token" \
                              -H "Content-Type: application/x-www-form-urlencoded" \
                              -d "grant_type=password" \
                              -d "client_id=${KEYCLOAK_CLIENT_ID}" \
                              -d "client_secret=${KEYCLOAK_CLIENT_SECRET}" \
                              -d "username=${TEST_USER_CREDS_USR}" \
                              -d "password=${TEST_USER_CREDS_PSW}")
                            
                            JWT_TOKEN=$(echo "$TOKEN_RES" | grep -o '"access_token":"[^"]*' | grep -o '[^"]*$')
 
                             # 3. Run k6 read-stress test (project-load-test.js)
                            FIRST_TENANT=$(echo "${TEST_TENANTS}" | cut -d',' -f1)
                            docker run --rm \
                              --network agency-os-net \
                              -e API_URL="http://agency-os:8080" \
                              -e JWT_TOKEN="$JWT_TOKEN" \
                              -e TENANT_ID="$FIRST_TENANT" \
                              -v "$(pwd)/stress-tests:/stress-tests" \
                              grafana/k6 run /stress-tests/project-load-test.js

                            # 3. Run k6 E2E workflow stress test (workflow-load-test.js)
                            docker run --rm \
                              --network agency-os-net \
                              -e API_URL="http://agency-os:8080" \
                              -e JWT_TOKEN="$JWT_TOKEN" \
                              -e TENANT_IDS="${TEST_TENANTS}" \
                              -v "$(pwd)/stress-tests:/stress-tests" \
                              grafana/k6 run /stress-tests/workflow-load-test.js
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
