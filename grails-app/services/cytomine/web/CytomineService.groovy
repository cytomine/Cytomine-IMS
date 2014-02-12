package cytomine.web

import be.cytomine.client.Cytomine

class CytomineService {

    def grailsApplication

    def getCytomine(String cytomineUrl) {
        String publicKey = grailsApplication.config.grails.imageServerPublicKey
        String privateKey = grailsApplication.config.grails.imageServerPrivateKey
        return new Cytomine(cytomineUrl, publicKey, privateKey, "./", false)
    }
}
