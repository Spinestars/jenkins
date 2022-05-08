package org.devops


// 代码扫描
def SonarScan(projectName, projectType, buildTools){
    //搜索项目
    result = SerarchProject(projectName)
    println(result)
    
    //判断项目是否存在
    if (result == "false"){
        println("${projectName}---项目不存在,准备创建项目---> ${projectName}！")
        CreateProject(projectName)
    } else {
        println("${projectName}---项目已存在！")
    }
    
    //配置项目质量规则
    qpName=projectName.split("-")[0]   //anyops
    //qpName="myjava"

    if (projectType == "npm"){
        ConfigQualityProfiles(projectName,"js",qpName)
        ConfigQualityProfiles(projectName,"ts",qpName)
    } else if (projectType == "maven") {
        ConfigQualityProfiles(projectName,"java",qpName)
    }
    

    //配置质量阈
    ConfigQualityGates(projectName,qpName)


    withSonarQubeEnv("mysonarserver"){
        def sonarDate = sh  returnStdout: true, script: 'date  +%Y%m%d%H%M%S'
        sonarDate = sonarDate - "\n"

        if (projectType == "maven") {
            sh """ 
                ${buildTools["sonar"]}/bin/sonar-scanner \
                -Dsonar.projectKey=${JOB_NAME.split("/")[1]} \
                -Dsonar.projectName=${JOB_NAME.split("/")[1]} \
                -Dsonar.projectVersion=${sonarDate} \
                -Dsonar.ws.timeout=30 \
                -Dsonar.projectDescription="my test project" \
                -Dsonar.links.homepage="${gitHttpURL}" \
                -Dsonar.sources=src \
                -Dsonar.sourceEncoding=UTF-8 \
                -Dsonar.java.binaries=target/classes \
                -Dsonar.java.test.binaries=target/test-classes \
                -Dsonar.java.surefire.report=target/surefire-reports  \
                
           """
        } else if ( projectType == "npm" ) {

            sh """ 
                ${buildTools["sonar"]}/bin/sonar-scanner \
                -Dsonar.projectKey=${JOB_NAME.split("/")[1]} \
                -Dsonar.projectName=${JOB_NAME.split("/")[1]} \
                -Dsonar.projectVersion=${sonarDate} \
                -Dsonar.ws.timeout=30 \
                -Dsonar.projectDescription="my test project" \
                -Dsonar.links.homepage="${gitHttpURL}" \
                -Dsonar.sources=src \
                -Dsonar.sourceEncoding=UTF-8 

            """
       }   
    }
    

    sleep 10
    //获取扫描结果
    result = GetProjectStatus(projectName)
    println(result)
    if (result.toString() == "ERROR"){
        error " 代码质量阈错误！请及时修复！"
    } else {
        println(result)
    }
}


def HttpReq(reqType,reqUrl,reqBody){
    def sonarServer = "http://192.168.1.200:9000/api"
   
    response = httpRequest authentication: '4675830a-4330-4dd6-9185-cf62161967f0',
            httpMode: reqType, 
            contentType: "APPLICATION_JSON",
            consoleLogResponseBody: true,
            ignoreSslErrors: true, 
            requestBody: reqBody,
            url: "${sonarServer}/${reqUrl}"
            //quiet: true
    
    return response
}

//搜索Sonar项目
def SerarchProject(projectName){
    apiUrl = "projects/search?projects=${projectName}"
    response = HttpReq("GET",apiUrl,'')

    response = readJSON text: """${response.content}"""
    result = response["paging"]["total"]

    if(result.toString() == "0"){
       return "false"
    } else {
       return "true"
    }
}

//获取Sonar质量阈状态
def GetProjectStatus(projectName){
    apiUrl = "project_branches/list?project=${projectName}"
    response = HttpReq("GET",apiUrl,'')
    
    response = readJSON text: """${response.content}"""
    result = response["branches"][0]["status"]["qualityGateStatus"]
    
    //println(response)
    
   return result
}



//创建Sonar项目
def CreateProject(projectName){
    apiUrl =  "projects/create?name=${projectName}&project=${projectName}"
    response = HttpReq("POST",apiUrl,'')
    println(response)
}

//配置项目质量规则

def ConfigQualityProfiles(projectName,lang,qpname){
    apiUrl = "qualityprofiles/add_project?language=${lang}&project=${projectName}&qualityProfile=${qpname}"
    response = HttpReq("POST",apiUrl,'')
    println(response)
}


//获取质量阈ID
def GetQualtyGateId(gateName){
    apiUrl= "qualitygates/show?name=${gateName}"
    response = HttpReq("GET",apiUrl,'')
    response = readJSON text: """${response.content}"""
    result = response["id"]
    
    return result
}

//配置项目质量阈

def ConfigQualityGates(projectName,gateName){
    gateId = GetQualtyGateId(gateName)
    apiUrl = "qualitygates/select?gateId=${gateId}&projectKey=${projectName}"
    response = HttpReq("POST",apiUrl,'')
    println(response)println(response)
}
