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
