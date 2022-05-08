package org.devops


// 构建函数
def Build(buildTools, buildType){
    switch(buildType){
        case "maven":
            sh "${buildTools["maven"]}/bin/mvn clean package"
            break
        case "gradle":
            sh "${buildTools["gradle"]}/bin/gradle build -x test"
            break
        
        case "golang":
            sh "${buildTools["golang"]}/bin/go build demo.go"
            break
        
        case "npm":
            sh """ ${buildTools["npm"]}/bin/npm install  && ${buildTools["npm"]}/bin/npm run build """
            break
        
        default :
            println("buildType ==> [maven|gralde|golang|npm]")
            break
    }
}






