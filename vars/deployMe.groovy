def call(Map pipelineParams) {
    def registry = pipelineParams.registry
    def registryCredential = pipelineParams.registryCredential
    def dockerImage = ''
    
    pipeline {
      agent any
      stages {
        stage('Cloning Git') {
          steps {
            script {
                gitCommit = git credentialsId: pipelineParams.gitCredential, branch: pipelineParams.branch, url: pipelineParams.git
            }
          }
        }
        stage('Building Image') {
          steps{
            script {
                dockerImage = docker.build registry + ":"+ gitCommit.GIT_COMMIT.take(7)+"-${BUILD_NUMBER}"
            }
          }
        }
        stage('Deploy Image to Registry') {
          steps{
            script {
              docker.withRegistry( 'https://' + registry, registryCredential ) {
                dockerImage.push()
              }
            }
          }
        }
        
        stage( "Deploy to Dev" ) {
          when {
            branch 'dev'
          }
          steps{
            script {
                withCredentials([string(credentialsId: "argocd-deploy-role", variable: 'ARGOCD_AUTH_TOKEN')]) {
                    sh "argocd --insecure --grpc-web --server=${pipelineParams.argocd} app set ${JOB_NAME} -p image.tag=${env.GIT_COMMIT.take(7)}-${BUILD_NUMBER}"
                    sh "argocd --insecure --grpc-web --server=${pipelineParams.argocd} app sync ${JOB_NAME}"
                    sh "argocd --insecure --grpc-web --server=${pipelineParams.argocd} app wait ${JOB_NAME} --timeout 600"
                }
              
            }
          } 
        }
        
        stage( "Deploy to Production" ) {
          when {
            branch 'master'
          }
          steps{
            script {
                withCredentials([string(credentialsId: "argocd-deploy-role", variable: 'ARGOCD_AUTH_TOKEN')]) {
                    sh "argocd --insecure --grpc-web --server=${pipelineParams.argocd} app set ${JOB_NAME} -p image.tag=${env.GIT_COMMIT.take(7)}-${BUILD_NUMBER}"
                    sh "argocd --insecure --grpc-web --server=${pipelineParams.argocd} app sync ${JOB_NAME}"
                    sh "argocd --insecure --grpc-web --server=${pipelineParams.argocd} app wait ${JOB_NAME} --timeout 600"
                }
              
            }
          } 
        }  

        stage('Remove Unused docker image') {
          steps{
            sh "docker rmi ${registry}:${gitCommit.GIT_COMMIT.take(7)}-${BUILD_NUMBER}"
          }
        }
      }
    }
}
