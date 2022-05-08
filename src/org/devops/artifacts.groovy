package org.devops


def Upload(repoName,fileName,fileTargetDir, fileDir){
    repoUrl = "http://192.168.1.200:8081/service/rest/v1/components?repository=${repoName}"

    if (fileName.endsWith(".jar")){
        types = "application/java-archive"
    } else if (fileName.endsWith(".tar.gz")){
        types = "application/x-gzip"
    } else {
        types = "application/java-archive"
    }

    sh """
        pwd
        cd ${fileDir}
        curl  -u admin:admin123 -X  POST "${repoUrl}"  \
        -H "accept: application/json" \
        -H "Content-Type: multipart/form-data" \
        -F "raw.directory=${fileTargetDir}" \
        -F "raw.asset1=@${fileName};type=${types}" \
        -F "raw.asset1.filename=${fileName}"
        cd ..
    """
}
