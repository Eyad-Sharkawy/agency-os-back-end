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

        stage('Deploy to Server') {
            when {
                branch 'main'
            }
            steps {
                // Best practice: Deploy from the stable deployment folder, not the ephemeral Jenkins workspace
                sh '''
                    # Copy the newly checked-out workspace files to the deployment folder (excluding target to save space)
                    rsync -av --exclude='.git' --exclude='target' ./ /home/ubuntu/agency-os-back-end/
                    
                    # Navigate to deployment directory and spin up containers
                    cd /home/ubuntu/agency-os-back-end
                    docker compose down
                    docker compose up -d --build
                '''
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
        }
    }
}
