pipeline {
    agent {
        kubernetes {
            yaml '''
                apiVersion: v1
                kind: Pod
                spec:
                  serviceAccountName: jenkins-admin
                  imagePullSecrets:
                  - name: dockerhub-credentials
                  containers:
                  - name: maven
                    image: maven:3.9.8-eclipse-temurin-21
                    command: ["cat"]
                    tty: true
                    resources:
                      requests:
                        cpu: "100m"       # 提高到 0.5 CPU
                        memory: "1024Mi"  # 提高到 1GB
                      limits:
                        cpu: "100m"       # 提高到 2 CPU cores
                        memory: "1024Mi"  # 提高到 2GB
                    volumeMounts:
                    - mountPath: /root/.m2
                      name: maven-repo
                    - mountPath: /home/jenkins/agent
                      name: workspace-volume
                    workingDir: /home/jenkins/agent
                  - name: docker
                    image: docker:23-dind
                    privileged: true
                    securityContext:
                      privileged: true
                    resources:
                      requests:
                        cpu: "25m"
                        memory: "256Mi"
                      limits:
                        cpu: "100m"
                        memory: "512Mi"
                    env:
                    - name: DOCKER_TLS_CERTDIR
                      value: ""
                    - name: DOCKER_BUILDKIT
                      value: "1"
                    volumeMounts:
                    - mountPath: /home/jenkins/agent
                      name: workspace-volume
                  - name: kubectl
                    image: bitnami/kubectl:latest
                    command: ["/bin/sh"]
                    args: ["-c", "while true; do sleep 30; done"]
                    imagePullPolicy: Always
                    securityContext:
                      runAsUser: 0
                    resources:
                      requests:
                        cpu: "10m"
                        memory: "128Mi"
                      limits:
                        cpu: "50m"
                        memory: "256Mi"
                    volumeMounts:
                    - mountPath: /home/jenkins/agent
                      name: workspace-volume
                  volumes:
                  - name: maven-repo
                    emptyDir: {}
                  - name: workspace-volume
                    emptyDir: {}
            '''
            defaultContainer 'maven'
            inheritFrom 'default'
        }
    }
    options {
        timestamps()
        disableConcurrentBuilds()
    }
    environment {
        DOCKER_IMAGE = 'papakao/ty-multiverse-gateway'
        DOCKER_TAG = "${BUILD_NUMBER}"
        K8S_NAMESPACE = 'default'
    }
    
    stages {
        stage('Clone and Setup') {
            steps {
                script {
                    container('maven') {
                        sh '''
                            # 確認 Dockerfile 存在
                            ls -la
                            if [ ! -f "Dockerfile" ]; then
                                echo "Error: Dockerfile not found!"
                                exit 1
                            fi
                        '''
                    }
                }
            }
        }

        stage('Build') {
            steps {
                container('maven') {
                    script {
                        withCredentials([string(credentialsId: 'GITHUB_TOKEN', variable: 'GITHUB_TOKEN')]) {
                            sh '''
                                # 創建 Maven settings.xml 以配置 GitHub Packages 認證
                                mkdir -p ~/.m2
                                cat > ~/.m2/settings.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>Vinskao</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF
                                # 執行 Maven 構建
                                MAVEN_OPTS="-Xmx1024m -XX:+UseG1GC" mvn -T 1C -Dmaven.javadoc.skip=true clean package -P platform -DskipTests
                            '''
                        }
                    }
                }
            }
        }

        stage('Test') {
            steps {
                container('maven') {
                    script {
                        withCredentials([string(credentialsId: 'GITHUB_TOKEN', variable: 'GITHUB_TOKEN')]) {
                            sh '''
                                # 創建 Maven settings.xml 以配置 GitHub Packages 認證
                                mkdir -p ~/.m2
                                cat > ~/.m2/settings.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>Vinskao</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF
                                # 執行 Maven 測試
                                MAVEN_OPTS="-Xmx1024m -XX:+UseG1GC" mvn -T 1C -Dmaven.javadoc.skip=true test -P platform
                            '''
                        }
                    }
                }
            }
        }
        
        stage('Debug Environment') {
            steps {
                container('kubectl') {
                    script {
                        echo "=== Listing all environment variables ==="
                        sh 'printenv | sort'
                    }
                }
            }
        }

        stage('Build Docker Image with BuildKit') {
            steps {
                container('docker') {
                    script {
                        withCredentials([
                            usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD'),
                            string(credentialsId: 'GITHUB_TOKEN', variable: 'GITHUB_TOKEN')
                        ]) {
                            sh '''
                                cd "${WORKSPACE}"
                                
                                # Verify GitHub token (show first 10 chars for debugging)
                                if [ -z "${GITHUB_TOKEN}" ]; then
                                    echo "ERROR: GITHUB_TOKEN is empty!"
                                    exit 1
                                fi
                                echo "GitHub token present (length: ${#GITHUB_TOKEN}, starts with: ${GITHUB_TOKEN:0:10}...)"
                                
                                # Docker login with retry mechanism
                                echo "Attempting Docker login..."
                                for i in {1..3}; do
                                    echo "Docker login attempt $i/3"
                                    if echo "${DOCKER_PASSWORD}" | docker login -u "${DOCKER_USERNAME}" --password-stdin; then
                                        echo "Docker login successful"
                                        break
                                    else
                                        echo "Docker login attempt $i failed"
                                        if [ $i -eq 3 ]; then
                                            echo "All Docker login attempts failed"
                                            exit 1
                                        fi
                                        echo "Waiting 10 seconds before retry..."
                                        sleep 10
                                    fi
                                done
                                
                                # 確認 Dockerfile 存在
                                ls -la
                                if [ ! -f "Dockerfile" ]; then
                                    echo "Error: Dockerfile not found!"
                                    exit 1
                                fi
                                
                                # 構建 Docker 鏡像（啟用 BuildKit 與多平台參數，傳遞 GitHub token）
                                echo "Building Docker image with GitHub token..."
                                docker build \
                                    --build-arg BUILDKIT_INLINE_CACHE=1 \
                                    --build-arg GITHUB_TOKEN="${GITHUB_TOKEN}" \
                                    --build-arg GITHUB_USERNAME="Vinskao" \
                                    --cache-from ${DOCKER_IMAGE}:latest \
                                    -t ${DOCKER_IMAGE}:${DOCKER_TAG} \
                                    -t ${DOCKER_IMAGE}:latest \
                                    .
                                    
                                # Push with retry mechanism
                                echo "Pushing Docker images..."
                                for i in {1..3}; do
                                    echo "Push attempt $i/3 for ${DOCKER_IMAGE}:${DOCKER_TAG}"
                                    if docker push ${DOCKER_IMAGE}:${DOCKER_TAG}; then
                                        echo "Successfully pushed ${DOCKER_IMAGE}:${DOCKER_TAG}"
                                        break
                                    else
                                        echo "Push attempt $i failed for ${DOCKER_IMAGE}:${DOCKER_TAG}"
                                        if [ $i -eq 3 ]; then
                                            echo "All push attempts failed for ${DOCKER_IMAGE}:${DOCKER_TAG}"
                                            exit 1
                                        fi
                                        echo "Waiting 10 seconds before retry..."
                                        sleep 10
                                    fi
                                done
                                
                                for i in {1..3}; do
                                    echo "Push attempt $i/3 for ${DOCKER_IMAGE}:latest"
                                    if docker push ${DOCKER_IMAGE}:latest; then
                                        echo "Successfully pushed ${DOCKER_IMAGE}:latest"
                                        break
                                    else
                                        echo "Push attempt $i failed for ${DOCKER_IMAGE}:latest"
                                        if [ $i -eq 3 ]; then
                                            echo "All push attempts failed for ${DOCKER_IMAGE}:latest"
                                            exit 1
                                        fi
                                        echo "Waiting 10 seconds before retry..."
                                        sleep 10
                                    fi
                                done
                            '''
                        }
                    }
                }
            }
        }
        
        stage('Deploy to Kubernetes') {
            steps {
                container('kubectl') {
                    withCredentials([
                        string(credentialsId: 'KEYCLOAK_AUTH_SERVER_URL', variable: 'KEYCLOAK_AUTH_SERVER_URL'),
                        string(credentialsId: 'PUBLIC_REALM', variable: 'PUBLIC_REALM'),
                        string(credentialsId: 'PUBLIC_CLIENT_ID', variable: 'PUBLIC_CLIENT_ID'),
                        string(credentialsId: 'KEYCLOAK_CREDENTIALS_SECRET', variable: 'KEYCLOAK_CREDENTIALS_SECRET'),
                        string(credentialsId: 'PUBLIC_TYMB_URL', variable: 'PUBLIC_TYMB_URL'),
                        string(credentialsId: 'PUBLIC_FRONTEND_URL', variable: 'PUBLIC_FRONTEND_URL'),
                        string(credentialsId: 'REDIS_HOST', variable: 'REDIS_HOST'),
                        string(credentialsId: 'REDIS_CUSTOM_PORT', variable: 'REDIS_CUSTOM_PORT'),
                        string(credentialsId: 'REDIS_PASSWORD', variable: 'REDIS_PASSWORD')
                    ]) {
                        withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                            script {
                                try {
                                    sh '''
                                        set -e

                                        # Ensure envsubst is available (try Debian then Alpine)
                                        if ! command -v envsubst >/dev/null 2>&1; then
                                          (apt-get update && apt-get install -y --no-install-recommends gettext-base ca-certificates) >/dev/null 2>&1 || true
                                          command -v envsubst >/dev/null 2>&1 || (apk add --no-cache gettext ca-certificates >/dev/null 2>&1 || true)
                                        fi

                                        # In-cluster auth via ServiceAccount (serviceAccountName: jenkins-admin)
                                        kubectl cluster-info

                                        # Ensure Docker Hub imagePullSecret exists in default namespace
                                        kubectl create secret docker-registry dockerhub-credentials \
                                          --docker-server=https://index.docker.io/v1/ \
                                          --docker-username="${DOCKER_USERNAME}" \
                                          --docker-password="${DOCKER_PASSWORD}" \
                                          --docker-email="none" \
                                          -n default \
                                          --dry-run=client -o yaml | kubectl apply -f -

                                        # Inspect manifest directory
                                        if [ -d "k8s" ]; then
                                            ls -la k8s/
                                        elif [ -d "ty-multiverse-gateway/k8s" ]; then
                                            ls -la ty-multiverse-gateway/k8s/
                                        fi

                                        echo "Recreating deployment ..."
                                        echo "=== Effective sensitive env values ==="
                                        echo "KEYCLOAK_AUTH_SERVER_URL=${KEYCLOAK_AUTH_SERVER_URL}"
                                        echo "PUBLIC_REALM=${PUBLIC_REALM}"
                                        echo "PUBLIC_FRONTEND_URL=${PUBLIC_FRONTEND_URL}"
                                        echo "REDIS_HOST=${REDIS_HOST}:${REDIS_CUSTOM_PORT}"

                                        # Update deployment image
                                        DEPLOYMENT_FILE=""
                                        if [ -f "k8s/deployment.yaml" ]; then
                                            DEPLOYMENT_FILE="k8s/deployment.yaml"
                                        elif [ -f "ty-multiverse-gateway/k8s/deployment.yaml" ]; then
                                            DEPLOYMENT_FILE="ty-multiverse-gateway/k8s/deployment.yaml"
                                        fi
                                        
                                        if [ -n "$DEPLOYMENT_FILE" ]; then
                                            kubectl set image deployment/ty-multiverse-gateway gateway=${DOCKER_IMAGE}:${DOCKER_TAG} -n default || \
                                            kubectl apply -f ${DEPLOYMENT_FILE} -n default
                                        else
                                            kubectl set image deployment/ty-multiverse-gateway gateway=${DOCKER_IMAGE}:${DOCKER_TAG} -n default
                                        fi

                                        # Create Secret with all gateway secrets (RabbitMQ uses hardcoded K8s service name)
                                        kubectl create secret generic ty-multiverse-gateway-secrets \
                                          --from-literal=KEYCLOAK_AUTH_SERVER_URL="${KEYCLOAK_AUTH_SERVER_URL}" \
                                          --from-literal=PUBLIC_REALM="${PUBLIC_REALM}" \
                                          --from-literal=PUBLIC_CLIENT_ID="${PUBLIC_CLIENT_ID}" \
                                          --from-literal=KEYCLOAK_CREDENTIALS_SECRET="${KEYCLOAK_CREDENTIALS_SECRET}" \
                                          --from-literal=PUBLIC_TYMB_URL="${PUBLIC_TYMB_URL}" \
                                          --from-literal=PUBLIC_FRONTEND_URL="${PUBLIC_FRONTEND_URL}" \
                                          --from-literal=REDIS_HOST="${REDIS_HOST}" \
                                          --from-literal=REDIS_CUSTOM_PORT="${REDIS_CUSTOM_PORT}" \
                                          --from-literal=REDIS_PASSWORD="${REDIS_PASSWORD}" \
                                          -n default --dry-run=client -o yaml | kubectl apply -f -

                                        kubectl rollout status deployment/ty-multiverse-gateway -n default
                                    '''

                                    // 檢查部署狀態
                                    sh 'kubectl get deployments -n default'
                                    sh 'kubectl rollout status deployment/ty-multiverse-gateway -n default'
                                } catch (Exception e) {
                                    echo "Error during deployment: ${e.message}"
                                    // Debug non-ready pods and recent events
                                    sh '''
                                        set +e
                                        echo "=== Debug: pods for ty-multiverse-gateway ==="
                                        kubectl get pods -n default -l app=ty-multiverse-gateway -o wide || true

                                        echo "=== Debug: describe all pods ==="
                                        for p in $(kubectl get pods -n default -l app=ty-multiverse-gateway -o jsonpath='{.items[*].metadata.name}'); do
                                          echo "--- Pod: $p ---"
                                          kubectl describe pod -n default "$p" || true
                                          echo "=== Last 200 logs for $p ==="
                                          kubectl logs -n default "$p" --tail=200 || true
                                          echo ""
                                        done

                                        echo "=== Recent events (default ns) ==="
                                        kubectl get events -n default --sort-by=.lastTimestamp | tail -n 100 || true
                                    '''
                                    throw e
                                }
                            } // end script
                        } // end inner withCredentials
                    } // end outer withCredentials
                } // end container
            } // end steps
        } // end stage
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

