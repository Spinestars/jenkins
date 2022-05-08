package org.devops


// 封装HTTP
def HttpReq(reqType, reqUrl,reqBody ){
    def gitServer = "http://192.168.1.200/api/v4"
    withCredentials([string(credentialsId: 'b6e2ee3b-a739-44bc-b7be-99f795fc74b1', variable: 'GITLABTOKEN')]) {
        response = httpRequest acceptType: 'APPLICATION_JSON_UTF8', 
                          consoleLogResponseBody: true, 
                          contentType: 'APPLICATION_JSON_UTF8', 
                          customHeaders: [[maskValue: false, name: 'PRIVATE-TOKEN', value: "${GITLABTOKEN}"]], 
                          httpMode: "${reqType}", 
                          url: "${gitServer}/${reqUrl}", 
                          wrapAsMultipart: false,
                          requestBody: "${reqBody}"

    }
    return response
}

//获取文件内容
def GetRepoFile(projectId,filePath, branchName ){
   //GET /projects/:id/repository/files/:file_path/raw
   apiUrl = "/projects/${projectId}/repository/files/${filePath}/raw?ref=${branchName}"
   response = HttpReq('GET', apiUrl, "")

   return response.content

}

//更新文件内容
def UpdateRepoFile(projectId,filePath,fileContent, branchName){
    apiUrl = "projects/${projectId}/repository/files/${filePath}"
    reqBody = """{"branch": "${branchName}","encoding":"base64", "content": "${fileContent}", "commit_message": "update a new file"}"""
    response = HttpReq('PUT',apiUrl,reqBody)
    println(response)

}

//创建文件
def CreateRepoFile(projectId,filePath,fileContent, branchName){
    apiUrl = "projects/${projectId}/repository/files/${filePath}"
    reqBody = """{"branch": "${branchName}","encoding":"base64", "content": "${fileContent}", "commit_message": "update a new file"}"""
    response = HttpReq('POST',apiUrl,reqBody)
    println(response)

}


