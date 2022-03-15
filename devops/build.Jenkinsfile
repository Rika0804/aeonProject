def pomVersion
pipeline {
    agent {
        label 'ssh-slaves'
    }
    environment {
        NEXUS_ENDPT = 'http://10.132.164.100:8081'
        JAVA_APP_NAME = 'coupon-batch-fs'
        OCP_APP_NAME = 'cpn-batch-fs'
    }
    stages {
        stage('pom version parse') {
            steps {
                script {
                    def MDL = readMavenPom(file: 'coupon-batch-fs/pom.xml')
                    pomVersion = MDL.getVersion()
                }
                echo 'version: ' + pomVersion
            }
        }
        stage('war build') {
            steps {
                dir('coupon-batch-fs/') {
                    sh "mvn versions:use-releases -Dmaven.repo.local=${WORKSPACE}/repo -DallowRangeMatching=true -DgenerateBackupPoms=false"
                    sh 'mvn -Dmaven.repo.local=${WORKSPACE}/repo clean package -DskipTests'
                }
            }
        }
        stage('war push') {
            steps {
                nexusPublisher(
                    nexusInstanceId: 'nexus-1', 
                    nexusRepositoryId: 'maven-releases', 
                    packages: [
                        [
                            $class: 'MavenPackage', 
                            mavenAssetList: [
                                [
                                    classifier: '', 
                                    extension: '', 
                                    filePath: "coupon-batch-fs/target/coupon-batch-fs.war"
                                ]
                            ], 
                            mavenCoordinate: [
                                artifactId: 'coupon-batch-fs', 
                                groupId: 'jp.co.aeoncredit.coupon', 
                                packaging: 'war', 
                                version: "${pomVersion}.${BUILD_NUMBER}"
                            ]
                        ]
                    ]
                )
            }
        }
        stage('git tagging') {
            steps {
                withCredentials([string(credentialsId: 'gitlab-accesstoken-pushpull', variable: 'GLAPI_TOKEN')]) {
                    sh "curl --request POST --header 'PRIVATE-TOKEN: ${GLAPI_TOKEN}' 'https://jp-tok.git.cloud.ibm.com/api/v4/projects/acs-coupon%2Fcoupon-batch-fs/repository/tags?tag_name=v${pomVersion}.${BUILD_NUMBER}&ref=release'"
                }
            }
        }
        stage('release package push') {
            steps {
                sh 'cp coupon-batch-fs/target/coupon-batch-fs.war devops/containerize/'
                dir('devops/containerize') {
                    sh "wget ${NEXUS_ENDPT}/repository/build-env-aid/drivers/oracle-jdbc/19.3/ojdbc8.jar"
                    sh "wget ${NEXUS_ENDPT}/repository/build-env-aid/drivers/oracle-jdbc/12.2.0.1/module.xml"
                }
                dir('devops') {
                    sh "tar zcvf relpkg-${OCP_APP_NAME}.tar.gz containerize"
                    withCredentials([usernameColonPassword(credentialsId: 'pipeline-user', variable: 'NX_CRED')]) {
                        sh "curl -v -u '${NX_CRED}'" +
                            " -X POST '${NEXUS_ENDPT}/service/rest/v1/components?repository=coupon-artifacts'" + 
                            " -F raw.directory=/${OCP_APP_NAME}/${pomVersion}.${BUILD_NUMBER}/${OCP_APP_NAME}/" +
                            " -F raw.asset1.filename=relpkg-${OCP_APP_NAME}-${pomVersion}.${BUILD_NUMBER}.tar.gz" +
                            " -F raw.asset1=@relpkg-${OCP_APP_NAME}.tar.gz"
                    }
                }
            }
        }
    }
    post {
        success {
            slackSend(
                color: '#42FF42', 
                message: "ビルド成功 - *coupon-batch-fs*"
            )
        }
        failure {
            slackSend(
                color: '#FF4242', 
                message: "ビルド失敗 - *coupon-batch-fs*"
            )
            emailext(
                body: """
                    coupon-batch-fs ビルドジョブは失敗しました
                    """,
                subject: "[Jenkins] coupon-batch-fs ビルドジョブは失敗しました ***",
                recipientProviders: [requestor()]
            )
        }

        always {
            cleanWs()
            sh "rm -rf ${HOME}/.m2/repository/"
        }
    }
}
