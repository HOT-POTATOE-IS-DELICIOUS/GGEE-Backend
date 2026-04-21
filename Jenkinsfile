pipeline {
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
spec:
  imagePullSecrets:
    - name: harbor-pull-secret
  containers:
    - name: gradle
      image: ${HARBOR_URL}/library/gradle:8.14.3-jdk21
      command:
        - cat
      tty: true
    - name: git
      image: ${HARBOR_URL}/library/alpine-git:2.47.2
      command:
        - cat
      tty: true
    - name: kaniko
      image: ${HARBOR_URL}/library/kaniko-executor:debug
      command:
        - cat
      tty: true
"""
        }
    }

    options {
        timestamps()
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
    }

    environment {
        SERVICE_NAME = 'ggee-backend'
        GITOPS_REPO = 'git@github.com:HOT-POTATOE-IS-DELICIOUS/GGEE-K8S.git'
        IMAGE_REPOSITORY = "${env.HARBOR_URL}/ggee/ggee-backend"
    }

    stages {
        stage('Init') {
            steps {
                script {
                    env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    if (env.BRANCH_NAME == 'main') {
                        env.DEPLOY_ENV = 'prod'
                        env.IMAGE_TAG = "prod-${env.GIT_COMMIT_SHORT}"
                        env.VALUES_FILE = 'apps/ggee-backend/chart/values-prod.yaml'
                        env.DEPLOY_ENABLED = 'true'
                    } else if (env.BRANCH_NAME == 'dev') {
                        env.DEPLOY_ENV = 'dev'
                        env.IMAGE_TAG = "dev-${env.GIT_COMMIT_SHORT}"
                        env.VALUES_FILE = 'apps/ggee-backend/chart/values-dev.yaml'
                        env.DEPLOY_ENABLED = 'true'
                    } else {
                        env.DEPLOY_ENV = 'ci'
                        env.IMAGE_TAG = "ci-${env.GIT_COMMIT_SHORT}"
                        env.VALUES_FILE = ''
                        env.DEPLOY_ENABLED = 'false'
                    }
                }
            }
        }

        stage('Test') {
            steps {
                container('gradle') {
                    withEnv([
                            'SPRING_R2DBC_URL=r2dbc:postgresql://localhost:5432/test',
                            'SPRING_R2DBC_USERNAME=test',
                            'SPRING_R2DBC_PASSWORD=test',
                            'KAFKA_BOOTSTRAP_SERVERS=localhost:9092',
                            'KAFKA_STREAMS_APPLICATION_ID=ggee-backend-ci',
                            'KAFKA_STREAMS_AUTO_STARTUP=false',
                            'SERVER_PORT=8080',
                            'CORS_PATH_PATTERN=/**',
                            'CORS_ALLOWED_ORIGINS=http://localhost:3000',
                            'CORS_ALLOWED_METHODS=GET,POST,PUT,PATCH,DELETE,OPTIONS',
                            'CORS_ALLOWED_HEADERS=*',
                            'CORS_ALLOW_CREDENTIALS=true',
                            'CORS_MAX_AGE=3600',
                            'JWT_ACCESS_TOKEN_ACTIVE_TIME=3600000',
                            'JWT_REFRESH_TOKEN_ACTIVE_TIME=1209600000',
                            'JWT_HEADER=Authorization',
                            'JWT_PREFIX=Bearer',
                            'JWT_SECRET_KEY=ZmFrZS1mYWtlLWZha2UtZmFrZS1mYWtlLWZha2UtZmFrZS1mYWtlLWZha2U=',
                            'SNOWFLAKE_WORKER_ID=1',
                            'GGEE_CRAWLER_RESULT_EVENT_TOPIC=crawl.result.control-plane',
                            'GGEE_CRAWLER_DEDUPLICATED_COMMENT_TOPIC=crawl.comment.deduped',
                            'GGEE_CRAWLER_DEDUP_STORE_NAME=ggee-crawler-dedup-store',
                            'GGEE_CRAWLER_DEDUP_TTL=PT24H',
                            'GGEE_CRAWLER_DEDUP_CLEANUP_INTERVAL=PT5M',
                            'GGEE_CRAWLER_JOB_CREATE_TOPIC=crawl.job.create'
                    ]) {
                        sh 'chmod +x gradlew'
                        sh './gradlew test --no-daemon'
                    }
                }
            }
        }

        stage('Build Image') {
            steps {
                container('kaniko') {
                    script {
                        if (env.DEPLOY_ENABLED == 'true') {
                            withCredentials([usernamePassword(credentialsId: 'harbor-credentials', usernameVariable: 'HARBOR_USER', passwordVariable: 'HARBOR_PASSWORD')]) {
                                sh '''
                                    mkdir -p /kaniko/.docker
                                    cat > /kaniko/.docker/config.json <<EOF
                                    {
                                      "auths": {
                                        "${HARBOR_URL}": {
                                          "username": "${HARBOR_USER}",
                                          "password": "${HARBOR_PASSWORD}"
                                        }
                                      }
                                    }
EOF
                                    /kaniko/executor \
                                      --context="${WORKSPACE}" \
                                      --dockerfile="${WORKSPACE}/Dockerfile" \
                                      --destination="${IMAGE_REPOSITORY}:${IMAGE_TAG}" \
                                      --build-arg "HARBOR_URL=${HARBOR_URL}" \
                                      --insecure \
                                      --insecure-pull \
                                      --skip-tls-verify \
                                      --skip-tls-verify-pull \
                                      --snapshot-mode=redo
                                '''
                            }
                        } else {
                            sh '''
                                /kaniko/executor \
                                  --context="${WORKSPACE}" \
                                  --dockerfile="${WORKSPACE}/Dockerfile" \
                                  --destination="${IMAGE_REPOSITORY}:${IMAGE_TAG}" \
                                  --build-arg "HARBOR_URL=${HARBOR_URL}" \
                                  --no-push \
                                  --snapshot-mode=redo
                            '''
                        }
                    }
                }
            }
        }

        stage('Update GitOps') {
            when {
                expression { env.DEPLOY_ENABLED == 'true' }
            }
            steps {
                container('git') {
                    sshagent(['github-credentials']) {
                        script {
                            def manifestDir = "${env.WORKSPACE}/gitops"
                            sh """
                                rm -rf ${manifestDir}
                                mkdir -p ~/.ssh
                                ssh-keyscan github.com >> ~/.ssh/known_hosts 2>/dev/null
                                git clone ${env.GITOPS_REPO} ${manifestDir}
                                chmod -R a+rwX ${manifestDir}
                            """

                            def valuesPath = "${manifestDir}/${env.VALUES_FILE}"
                            def values = readYaml file: valuesPath
                            values.image.repository = env.IMAGE_REPOSITORY
                            values.image.tag = env.IMAGE_TAG
                            writeYaml file: valuesPath, data: values, overwrite: true

                            sh """
                                cd ${manifestDir}
                                git config user.email "jenkins@hotpotato.local"
                                git config user.name "Jenkins CI"
                                git add ${env.VALUES_FILE}
                                git diff --cached --quiet || git commit -m "chore(cicd): bump ${env.SERVICE_NAME} ${env.DEPLOY_ENV} image to ${env.IMAGE_TAG}"
                                git push origin main
                            """
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    cleanWs(deleteDirs: true, disableDeferredWipeout: true, notFailBuild: true)
                } catch (err) {
                    echo "skip cleanWs: ${err.getClass().getSimpleName()}"
                }
            }
        }
    }
}
