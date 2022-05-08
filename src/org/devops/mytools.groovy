package org.devops



//下载代码
def GetCode(srcType,branchName,gitHttpURL,jenkinsGitAuthId){

    if (srcType == "git"){
        println("下载代码 --> 分支： ${branchName}")
        str2 = gitHttpURL.split("/|\\.")
        gitName = str2[-2]
        checkout([$class: 'GitSCM', 
                    branches: [[name: "${branchName}"]],
                    extensions: [],  //extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "docker/${gitName}"]] 将代码克隆到 docker/xxx 目录
                    userRemoteConfigs: [[
                        credentialsId: "${jenkinsGitAuthId}", 
                        url: "${gitHttpURL}"
                    ]]
                ])
    }

}

//下载代码,但只下载git项目中的子目录代码
def GetCodeSubmodule(srcType,branchName,gitHttpURL,moduleName){

    if (srcType == "git"){
        println("下载代码 --> 分支： ${branchName}")
        str2 = gitHttpURL.split("/|\\.")
        gitName = str2[-2]
        checkout([$class: 'GitSCM', branches: [[name: "${branchName}"]],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [[$class: 'SparseCheckoutPaths', 
                                sparseCheckoutPaths: [[path: "${moduleName}"]]]], 
                    submoduleCfg: [],  
                    userRemoteConfigs: [[credentialsId: 'b874381c-20e9-4578-be9d-f1f691c25b23', 
                                        url: "${gitHttpURL}"]]])
    }

}

// 格式化输出, 需要AnsiColor插件支持.
// 接收内容，并输出彩色内容
def myPrint(content, color){
    colors = ['red'   : "\033[40;31m #############${content}############# \033[0m",
        'green' : "\033[40;32m #############${content}############# \033[0m",
        'yellow' : "\033[40;33m #############${content}############# \033[0m",
        'blue'  : "\033[47;34m #############${content}############# \033[0m"
        ]
    ansiColor('xterm') {
        println(colors[color])
    }
}

// 邮件通知
def EmailUser(userEmail,status){
 	emailext body: """
            <!DOCTYPE html> 
            <html> 
            <head> 
            <meta charset="UTF-8"> 
            </head> 
            <body leftmargin="8" marginwidth="0" topmargin="8" marginheight="4" offset="0"> 
                <img src="http://<jenkins>/static/0eef74bf/images/headshot.png">
                <table width="95%" cellpadding="0" cellspacing="0" style="font-size: 11pt; font-family: Tahoma, Arial, Helvetica, sans-serif">   
                    <tr> 
                        <td><br /> 
                            <b><font color="#0B610B">构建信息</font></b> 
                        </td> 
                    </tr> 
                    <tr> 
                        <td> 
                            <ul> 
                                <li>项目名称：${JOB_NAME}</li>         
                                <li>构建编号：${BUILD_ID}</li> 
                                <li>构建状态: ${status} </li>                         
                                <li>项目地址：<a href="${BUILD_URL}">${BUILD_URL}</a></li>    
                                <li>构建日志：<a href="${BUILD_URL}console">${BUILD_URL}console</a></li> 
                            </ul> 
                        </td> 
                    </tr> 
                    <tr>  
                </table> 
            </body> 
            </html>  """,
            subject: "Jenkins-${JOB_NAME}项目构建信息 ",
            to: userEmail
}

// 其他chat通知
// 封装chatBot请求， 需要 http request 插件
// 接收http method、http地址、http请求体，返回请求结果
def myChat(reqMode,reqUrl,reqBody){
    result = httpRequest httpMode: reqMode,
                accept: "APPLICATION_JSON_UTF8",
                contentType: "APPLICATION_JSON_UTF8",
                consoleLogResponseBody: true,
                ignoreSslErrors: true,
                requestBody: reqBody,
                url: reqUrl
                quiet: true
    return result
}

// ansible 远程执行
// 需要提前部署好私钥 /root/.remote.pem
// 对应的公钥需要放置在目标机器的 ${remoteUser} 用户中
// .hosts 存放在工作目录下
def ansible(remoteUser,shellCommand) {
    sh """
        ansible -i .hosts --private-key /root/.remote.pem -u ${remoteUser} all -m shell -a "${shellCommand}"
    """
}


