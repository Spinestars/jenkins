@Library("devopslib@master") _

def mytools = new org.devops.mytools()
def builds  = new org.devops.builds()
def sonar   = new org.devops.sonarqube()

def buildTools = [  "maven" : "/usr/local/apache-maven-3.8.1",
                    "gradle": "/usr/local/gradle-6.8.3/",
                    "golang": "/usr/local/go",
                    "web"   : "/usr/local/node-v14.16.1-linux-x64/",
                    "sonar" : "/usr/local/sonar-scanner-4.6.0.2311-linux/"]

//UI上面的参数
String branchName = "${env.branchName}"
String gitHttpURL = "${env.gitHttpURL}"
String buildType  = "${env.buildType}"
String skipSonar  = "${env.skipSonar}"

pipeline {
    agent { label  "build" }    
    options {
        skipDefaultCheckout true
    }

    stages {
        stage("GetCode"){
            steps{
                script{
                    mytools.GetCode("git",branchName,gitHttpURL)
                }
            }
            
        }

        stage("Build"){
            steps {
                script {
                    builds.Build(buildTools, buildType)
                }
            }
        }

        stage("SonarScan"){
            when {
                environment name: 'skipSonar', value: 'false'
            }
            steps{
                script{

                    projectName = "devops-maven-service"
                    //搜索项目
                    result = sonar.SerarchProject("${projectName}")
                    println(result)
                    
                    //判断项目是否存在
                    if (result == "false"){
                        println("${projectName}---项目不存在,准备创建项目---> ${projectName}！")
                        sonar.CreateProject("${projectName}")
                    } else {
                        println("${projectName}---项目已存在！")
                    }
                    
                    //配置项目质量规则
                    qpName="${projectName}".split("-")[0]   //Sonar%20way
                    qpName="myjava"
                    sonar.ConfigQualityProfiles("${projectName}","java",qpName)
                
                    //配置质量阈
                    sonar.ConfigQualityGates("${projectName}",qpName)

                    // 扫描
                    sh """
                        ${buildTools["sonar"]}/bin/sonar-scanner -Dproject.settings=sonar.properties \
                        -Dsonar.login=admin \
                        -Dsonar.password=admin \
                        -Dsonar.host.url=http://192.168.1.200:9000
                       """
                    
                    sleep 30
                    //获取扫描结果
                    result = sonar.GetProjectStatus("${projectName}")
                    println(result)
                    if (result.toString() == "ERROR"){
                        error " 代码质量阈错误！请及时修复！"
                    } else {
                        println(result)
                    }
                }
            }
        }

        stage("PushArtifacts"){
            when {
                environment name: 'buildType', value: 'maven'
            }
            steps{
                script{

                    def jarName = sh returnStdout: true, script: "cd target;ls *.jar"
                    jarName = jarName - "\n"

                    sh """
                        mvn deploy:deploy-file \
                        -DgeneratePom=false \
                        -DrepositoryId=maven-hosted \
                        -Durl=http://192.168.1.200:8081/repository/maven-snapshots/ \
                        -DpomFile=pom.xml \
                        -Dfile=target/${jarName}
                        """
                    
                    nexusArtifactUploader artifacts: [[ artifactId: 'myapp', 
                                                        classifier: '', 
                                                        file: './target/demo-0.0.1-SNAPSHOT.jar', 
                                                        type: 'jar']], 
                                        credentialsId: 'cabaa495-918b-4e73-9090-eafd5ac98c1b',
                                        groupId: 'com.devops', 
                                        nexusUrl: '192.168.1.200:8081', 
                                        nexusVersion: 'nexus3', 
                                        protocol: 'http', 
                                        repository: 'maven-hosted', 
                                        version: '1.1.5'
                }
            }
        }
        stage("SonarScanForPlugin"){
            steps{
                script{
                    withSonarQubeEnv("mysonarserver"){
                        def sonarDate = sh  returnStdout: true, script: 'date  +%Y%m%d%H%M%S'
                        sonarDate = sonarDate - "\n"

                        sh """ 
                                ${buildTools["sonar"]}/bin/sonar-scanner \
                                -Dsonar.projectKey=${JOB_NAME} \
                                -Dsonar.projectName=${JOB_NAME} \
                                -Dsonar.projectVersion=${sonarDate} \
                                -Dsonar.ws.timeout=30 \
                                -Dsonar.projectDescription="my test project" \
                                -Dsonar.links.homepage=http://www.baidu.com \
                                -Dsonar.sources=src \
                                -Dsonar.sourceEncoding=UTF-8 \
                                -Dsonar.java.binaries=target/classes \
                                -Dsonar.java.test.binaries=target/test-classes \
                                -Dsonar.java.surefire.report=target/surefire-reports  \
                                
                           """
                    }
                }
            }
        }
    }

    post {
        always {
            script{
                echo "always......"

            }
        }

        success {
            script {
                echo "success....."
            }
        }
    }

}

