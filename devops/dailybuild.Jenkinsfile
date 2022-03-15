def pomVersion
pipeline {
    agent {
        label 'ssh-slaves'
    }

    environment {
        NEXUS_ENDPT = 'http://10.132.164.100:8081'
        APP_NAME = 'coupon-batch-fs'
    }

    stages {
        stage ('pom set version'){
            steps {
                dir('coupon-batch-fs/') {
                    script {
                        pomVersion = sh script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true
                    }
                    sh "mvn versions:set -DnewVersion=${pomVersion}-SNAPSHOT -DgenerateBackupPoms=false"
                }
                echo 'version: ' + pomVersion + '-SNAPSHOT'
            }
        }
        stage('war build') {
            steps {
                dir('coupon-batch-fs/') {
                    sh 'mvn -Dmaven.repo.local=${WORKSPACE}/repo clean deploy -DaltDeploymentRepository=nexus::default::http://10.132.164.100:8081/repository/maven-snapshots'
                }
            }
        }
        stage('SonarQube analysis') {
            steps {
                sh 'sonar-scanner -Dproject.settings=devops/sonar-project.properties'
            }
        }
    }
    post {
        success {
            slackSend(
                color: '#42FF42', 
                message: "日次ビルド成功 - *${APP_NAME}*"
            )
        }
        failure {
            slackSend(
                color: '#FF4242', 
                message: "日次ビルド失敗 - *${APP_NAME}*"
            )
            emailext(
                body: """
                    ${APP_NAME} 日次ビルドジョブは失敗しました
${BUILD_URL}

ジョブ名: ${JOB_NAME}
ビルド番号: ${BUILD_NUMBER}
スレーブノード名: ${NODE_NAME}
                    """,
                subject: "[Jenkins] ${APP_NAME} 日次ビルドジョブは失敗しました ***",
                to: 'acscpn-ibm-cicd@wwpdl.vnet.ibm.com,acs_cpn@cresco.co.jp'
            )
        }
        always {
            cleanWs()
            sh "rm -rf ${HOME}/.m2/repository/"
        }
    }
}
