node {
    def mvnHome
    def artiServer
    def rtMaven
    def buildInfo
    def tagName
    stage('Prepare') {
        // Variables initilization
        artiServer = Artifactory.server('artiha-demo')
        buildInfo = Artifactory.newBuildInfo()
        // Build Env
        buildInfo.env.capture = true
        rtMaven = Artifactory.newMavenBuild()
        rtMaven.tool = "maven"

        // Specific dependency resolve repo
        rtMaven.resolver releaseRepo: 'libs-release', snapshotRepo: 'libs-snapshot', server: artiServer
        // Specific target repo
        rtMaven.deployer releaseRepo: 'libs-release-local', snapshotRepo: 'libs-snapshot-local', server: artiServer

        // Remove resources created previous time
        // TODO: change to try-catch
        try {
            sh 'kubectl -s kube-master:8080 --namespace=devops delete deploy --all'
            sh 'kubectl -s kube-master:8080 --namespace=devops delete svc --all'
            sh 'kubectl -s kube-master:8080 --namespace=devops delete configmap --all'
            sh 'sleep 15'
        } catch(Exception e) {
            println('remove resources in kubernetes failed, please check the log.')
        }
        
    }
    stage('SCM') {
        // Checkout source code
        // Notice：MUST PULL WITH AUTH
        git([url: 'https://gitlab.com/fuhui/kube-pipeline.git', branch: 'master', credentialsId: 'justin-gitlab2'])
    }
    stage('Sonar') {
        // Sonar scan
        def scannerHome = tool 'sonarClient';
        withSonarQubeEnv('sonar') {
            // TODO: Change to specific execute parameter
            sh "${scannerHome}/bin/sonar-runner"
        }
    }
    stage('Build') {
        // Maven build
        // TODO: Change to Jacoco agent
        rtMaven.run pom: 'pom.xml', goals: 'clean test install', buildInfo: buildInfo
    }
    stage('Image') {
        // Docker tag and upload to snapshot repository
        tagName = 'docker-snapshot-local.demo.jfrogchina.com/jfrog-cloud-demo:' + env.BUILD_NUMBER
        docker.build(tagName)
        // TODO: Change to artifactory server object
        def artDocker= Artifactory.docker('admin', 'ACWmSmLjLc5VKVYuSeumtarKV7TfboRAEwC1tqKAUvbniFJqp7xLfCyvJ7GxWuJZ')
        artDocker.push(tagName, 'docker-snapshot-local', buildInfo)
        artiServer.publishBuildInfo buildInfo
    }
    stage('Test') {
        // Smoke test
        docker.image(tagName).withRun('-p 8181:8080') { c->
            sleep 5
            // NOTE: According to business logic
            sh 'curl "http://127.0.0.1:8181"'
        }
    }
    stage('Prompte') {
        // Promote docker image from snapshot to release
        // TODO: Change to promote config
        sh 'sed -i "s/{tag}/${BUILD_ID}/g" docker-promote.json'
        sh 'curl  -X POST -H \"Content-Type: application/json\"  http://demo.jfrogchina.com/artifactory/api/docker/docker-snapshot-local/v2/promote -d @docker-promote.json -u admin:AKCp2WXCWmSmLjLc5VKVYuSeumtarKV7TioZfboRAEwC1tqKAUvbniFJqp7xLfCyvJ7GxWuJZ' 
    }
    stage('Config'){
        // Checkout application cofiguration from cofig repo and init in kubernetes
        sh 'curl -O -u admin:AKCp2WXCWmSmLjLc5VKVYuSeumtarKV7TqKAUvbniFJqp7xLfCyvJ7GxWuJZ -X GET http://demo.jfrogchina.com/artifactory/kube-config/1.0/app.cfg'
        sh 'kubectl -s kube-master:8080 --namespace=devops create configmap app-config --from-literal=$(cat app.cfg)'
    }
    stage('Kubernetes Deploy') {
        // Deploy to Kubernetes
        // TODO: Change kube-app.json to artifactory
        sh 'echo $(pwd)'
        sh 'sed -i "s/{tag}/${BUILD_ID}/g" kube-app.json'
        sh 'sleep 10'
        sh 'kubectl -s kube-master:8080 create -f kube-svc.json'
        sh 'kubectl -s kube-master:8080 create -f kube-app.json'
        sh 'for i in {1..120}; do echo "waiting for app starting,$[180-$i] second left..."; sleep 1; done;'
        sh 'echo deploy finished successfully.'
    }
    stage('Metadata') {
        // Binding metadata to docker images
        sh 'curl -X PUT \"http://demo.jfrogchina.com/artifactory/api/storage/docker-release-local/jfrog-cloud-demo/${BUILD_ID}?properties=build.name=Cloud-Native-Demo-01;build.version=${BUILD_ID};ut=paas;ut.passRate=1;sonar=done;sonarUrl=http://47.93.114.82:9000/dashboard/index/jfrog:kube-demo;envType=kube;env.namespace=devops;cfgVersion=1.0\" -u admin:AKCp2WXCWmSmLjLc5VKVYuSeumtarKV7TioZfboRAEwC1tqKAUvbniFJqp7xLfCyvJ7GxWuJZ'
    }
    stage('Pong') {
        // Echo happy pass message
        // TODO：Add summary
        echo 'please visit http://39.106.21.94:8181 to verify the result.'
    }
}
