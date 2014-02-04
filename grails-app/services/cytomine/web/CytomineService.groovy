package cytomine.web

import be.cytomine.client.Cytomine

class CytomineService {

    def grailsApplication

    def getCytomine() {
        String cytomineUrl = "http://localhost:8080/"
        String pubKey = grailsApplication.config.grails.imageServerPublicKey
        String privKey = grailsApplication.config.grails.imageServerPrivateKey
        return new Cytomine(cytomineUrl, pubKey, privKey, "./", false)
    }
}
