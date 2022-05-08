package org.devops


// 需要先安装 jenkins 插件: SonarQube Scanner
// 代码中涉及sonarqube api调用，因此需要先创建 jenkins 凭据，类型：username/password

// 代码扫描, 接收3个参数：项目/程序名（用来查找或者创建sonarqube项目），打包类型（maven/npm等，用来映射判断语言是什么），打包工具集（里面定义了sonar-scanner工具的家目录)
def SonarScan(projectName, projectType, buildTools){
    //搜索项目， 返回项目是否存在的结果
    result = SerarchProject(projectName)
    println(result)
    
    //判断项目是否存在
    if (result == "false"){
        println("${projectName}---项目不存在,准备创建项目---> ${projectName}！")
        CreateProject(projectName)
    } else {
        println("${projectName}---项目已存在！")
    }
    
    // 配置项目质量规则,从项目名里获取.
    // 项目名规则： 质量规则-项目名
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


    // 这个DSL关键词 withSonarQubeEnv 由 jenkins 插件: SonarQube Scanner 提供，用于关联 jenkins web 里定义 SonarQube 服务器地址
    // 开始扫描
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
    
    // 这个等待时间是必要的。因为从sonarqube获取的扫描结果可能并不是刚扫描出的结果，这个扫描出的结果是有延迟的。
    sleep 10

    //获取扫描结果, 如果超出质量阈，则执行 error 关键词，推出流水线。
    result = GetProjectStatus(projectName)
    println(result)
    if (result.toString() == "ERROR"){
        mytools.EmailUser(userEmail,"代码质量阈错误")
        error " 代码质量阈错误！请及时修复！"
    } else {
        println(result)
    }
}

// 下方是附属函数

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
// 这个接口从 sonarqube 项目详情页里通过F12就可以看到，是一个get请求,并返回json字符串，包含质量阈的结果
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
