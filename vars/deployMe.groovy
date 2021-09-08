def call(Map pipelineParams) {
    def registry = pipelineParams.registry
    def registryCredential = pipelineParams.registryCredential
    def dockerImage = ''
    def helmRepo = pipelineParams.helmRepo
    def helmPackage = pipelineParams.basePackage
    def serviceName = pipelineParams.serviceName
    def helmReleaseNote = 'release-notes'
    def releaseNotes = pipelineParams.withReleaseNotes
    def gitCommit = ''

    echo pipelineParams.helmRepo
    echo helmRepo
    echo pipelineParams.serviceName
    echo serviceName
    
    podTemplate(name: 'finex-argocd', label: 'finex-argocd', containers: [
        containerTemplate(name: 'builder', image: 'golang:1.10.3', ttyEnabled: true, command: 'cat', args: ''),
        containerTemplate(name: 'docker', image: 'docker:17.09', ttyEnabled: true, command: 'cat', args: '' ),
        containerTemplate(name: 'argo-cd-tools', image: 'argoproj/argo-cd-tools:latest', ttyEnabled: true, command: 'cat', args: '', envVars:[envVar(key: 'GIT_SSH_COMMAND', value: 'ssh -o StrictHostKeyChecking=no')] ),
        containerTemplate(name: 'argo-cd-cli', image: 'argoproj/argocd-cli:v0.7.1', ttyEnabled: true, command: 'cat', args: '', envVars:[envVar(key: 'ARGOCD_SERVER', value: pipelineParams.argocdServer)] ),
        ]
      )
    
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
        stage('Building image') {
          steps{
            script {
                dockerImage = docker.build registry + ":"+ gitCommit.GIT_COMMIT.take(7)+"-${BUILD_NUMBER}"
            }
          }
        }
        stage('Deploy Image') {
          steps{
            script {
              docker.withRegistry( 'https://' + registry, registryCredential ) {
                dockerImage.push()
              }
            }
          }
        }
        
//         stage( "Deploy ArgoCD" ) 
//             container('argo-cd-cli') {
//                 withCredentials([string(credentialsId: "argocd-deploy-role", variable: 'ARGOCD_AUTH_TOKEN')]) {
//                     sh "/argocd app set ${APP_NAME} -p image.tag=${gitCommit.GIT_COMMIT.take(7)}-${BUILD_NUMBER}"
//                     sh "/argocd app sync ${APP_NAME}"
//                     sh "/argocd app wait ${APP_NAME} --timeout 600"
//                 }
//             }
//         }  
//         stage('Check Release Notes condition') {
//           steps{
//             script {
//                 if (releaseNotes.length() > 100) {
//                     def dockerfile = 'Docs.Dockerfile'
//                     def docsImage = docker.build("${registry}-release-notes:${BUILD_TAG}", "-f ${dockerfile} .")

//                     docker.withRegistry( '', registryCredential ) {
//                         docsImage.push()
//                     }
//                     sh "helm upgrade --install ${serviceName}-release-notes ${helmRepo}/${helmReleaseNote} --set image.repository=${registry}-release-notes --set image.tag=$BUILD_TAG --set fullnameOverride=${serviceName}-release-notes"

//                     sh "docker rmi ${registry}-release-notes:${BUILD_TAG}"
//                 } else {
//                     sh "echo 'Release notes is not enough to release'"
//                 }
//             }
//           }
//         }
//         stage('Update Helm') {
//           steps{
//               sh "helm upgrade --install ${serviceName} ${helmRepo}/${helmPackage} --set image.repository=${registry} --set image.tag=$BUILD_TAG --set fullnameOverride=${serviceName}-${helmPackage}"
//           }
//         }
//         stage('Remove Unused docker image') {
//           steps{
//             sh "docker rmi ${registry}:${BUILD_TAG}"
//           }
//         }
      }
    }
}
