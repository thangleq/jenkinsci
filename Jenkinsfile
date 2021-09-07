node {

    pipelineParams = {
        registry = ''
        registryCredential = ''
        dockerImage = ''
        helmRepo = ''
        helmPackage = ''
        serviceName = ''
        helmReleaseNote = 'release-notes'
        releaseNotes = ''
    }
    
    def externalCall = load("vars/deployMe.groovy")

    externalCall(pipelineParams)
}
