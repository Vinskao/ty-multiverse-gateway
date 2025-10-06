pipeline {
    agent any
    
    environment {
        DOCKER_REGISTRY = 'your-docker-registry'
        IMAGE_NAME = 'ty-multiverse-gateway'
        K8S_NAMESPACE = 'ty-multiverse'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build Maven') {
            steps {
                dir('ty-multiverse-gateway') {
                    sh 'mvn clean package -DskipTests -Pplatform'
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                dir('ty-multiverse-gateway') {
                    script {
                        def imageTag = "${env.BUILD_NUMBER}"
                        sh """
                            docker build -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${imageTag} .
                            docker tag ${DOCKER_REGISTRY}/${IMAGE_NAME}:${imageTag} ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest
                        """
                    }
                }
            }
        }
        
        stage('Push Docker Image') {
            steps {
                script {
                    def imageTag = "${env.BUILD_NUMBER}"
                    sh """
                        docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${imageTag}
                        docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest
                    """
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                dir('ty-multiverse-gateway/k8s') {
                    sh """
                        kubectl apply -f deployment.yaml -n ${K8S_NAMESPACE}
                        kubectl rollout status deployment/ty-multiverse-gateway -n ${K8S_NAMESPACE}
                    """
                }
            }
        }
    }
    
    post {
        success {
            echo 'Gateway deployment successful!'
        }
        failure {
            echo 'Gateway deployment failed!'
        }
        always {
            cleanWs()
        }
    }
}

