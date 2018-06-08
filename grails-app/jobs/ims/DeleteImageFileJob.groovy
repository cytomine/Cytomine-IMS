package ims

import be.cytomine.client.Cytomine
import be.cytomine.client.collections.Collection
import be.cytomine.client.models.DeleteCommand
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONElement


class DeleteImageFileJob {
    static triggers = {
    }

    def grailsApplication

    def execute() {

        String cytomineUrl = grailsApplication.config.cytomine.coreURL

        String pubKey = grailsApplication.config.cytomine.imageServerPublicKey
        String privKey = grailsApplication.config.cytomine.imageServerPrivateKey

        Cytomine cytomine = new Cytomine((String) cytomineUrl, pubKey, privKey)

        long timeMargin = Long.parseLong(grailsApplication.config.cytomine.deleteImageFilesFrequency)*2

        //max between frequency*2 and 48h
        timeMargin = Math.max(timeMargin, 172800000L)

        DeleteCommandCollection commands = cytomine.getDeleteCommandByDomainAndAfterDate("uploadedFile", (new Date().time-timeMargin).toString())

        for(int i = 0; i<commands.size(); i++) {
            DeleteCommand command = commands.get(i)

            JSONElement j = JSON.parse(command.get("data"));

            File fileToDelete = new File(j.path+j.filename)

            if(fileToDelete.exists()) {
                log.info "DELETE file "+fileToDelete.absolutePath
                fileToDelete.delete()
            }

        }
    }
}
