pipeline {
    agent {
        label 'ssh-slaves'
    }

    parameters {
        string defaultValue: '', description: '', name: 'APP_VERSION', trim: true
    }

    environment {
        JAVA_APP_NAME = 'coupon-batch-fs'
        APP_VER = 'v1'
        APP_NAME = 'cpn-batch-fs'
        APP_PROJ = 'cpn-bat-main'
        OCP_APP_DESIG = "${APP_NAME}-${APP_VER}"
        OCP_SA_CRED = "ocp-${APP_PROJ}-jenkins-token"
        RELPKG_NAME = "relpkg-${APP_NAME}-${APP_VERSION}.tar.gz"
    }

    stages {
        stage('collect resources') {
            steps {
                sh 'mkdir container-binary'
                dir('container-binary') {
                    withCredentials([usernamePassword(credentialsId: 'nexus-pipeline-passwd', passwordVariable: 'NX_PASS', usernameVariable: 'NX_USER')]) {
                        sh "wget -q -nv --http-user=${NX_USER} --http-passwd=${NX_PASS} ${NEXUS_URL}/coupon-artifacts/${APP_NAME}/${APP_VERSION}/${APP_NAME}/relpkg-${APP_NAME}-${APP_VERSION}.tar.gz"
                        sh "wget -q -nv --http-user=${NX_USER} --http-passwd=${NX_PASS} ${NEXUS_URL}/coupon-artifacts/scripts/CK_RUN_JOB_SCRIPT.sh"
                        sh "wget -q -nv --http-user=${NX_USER} --http-passwd=${NX_PASS} ${NEXUS_URL}/coupon-artifacts/scripts/CK_ENTRYPOINT.sh"
                    }
                    sh "tar zxvf ${RELPKG_NAME} --strip-components 1"
                    sh 'chmod 664 ./*'
                    sh 'chmod 775 ./CK_RUN_JOB_SCRIPT.sh ./CK_ENTRYPOINT.sh'
                    sh "rm ${RELPKG_NAME}"
                }
            }
        }
        stage('faketime enabler') {
            when {
                expression {
                    return params.FAKETIME
                }
            }
            steps {
                dir('container-binary') {
                    sh 'mv Dockerfile Dockerfile.bak'
                    sh 'cat Dockerfile.bak | sed -e "1s/eap72-openjdk11-openshift-rhel8/jboss-faketime/" > Dockerfile'
                    sh 'rm Dockerfile.bak'
                    sh 'chmod 664 ./Dockerfile'
                    sh "echo FAKETIME=${FAKETIME_VALUE} >> ${ENV_NAME}.env"
                }
            }
        }
        stage('deploy app') {
            steps {
                dir('container-binary') {
                    withCredentials([string(credentialsId: "${OCP_SA_CRED}", variable: 'SA_TOKEN')]) {
                        sh "oc login ${OC_LOGIN_ENDPT} --token=${SA_TOKEN}"
                    }
                    sh "oc create configmap ${OCP_APP_DESIG}-env -n ${APP_PROJ} --from-env-file=${ENV_NAME}.env --dry-run -o yaml > new-env.yaml"
                    sh "oc apply -n ${APP_PROJ} -f new-env.yaml"
                    sh "oc start-build ${OCP_APP_DESIG} -n ${APP_PROJ} --from-dir=. --wait --follow"
                    sh "oc logout"
                }
            }
        }
    }
    post {
        success {
            script {
                if (env.ENV_NAME == 'dev_ita') {
                    slackSend(
                        color: '#42FF42', 
                        message: "デプロイ成功 - *${APP_NAME}*"
                    )
                }
            }
        }
        failure {
            script {
                if (env.ENV_NAME == 'dev_ita') {
                    slackSend(
                        color: '#FF4242', 
                        message: "デプロイ失敗 - *${APP_NAME}*"
                    )
                    emailext(
                        body: """
                            ${APP_NAME} デプロイジョブは失敗しました
                            """,
                        subject: "[Jenkins] ${APP_NAME} デプロイジョブは失敗しました ***",
                        recipientProviders: [requestor()]
                    )
                }
            }
        }
        cleanup {
            cleanWs()
        }
    }
}
