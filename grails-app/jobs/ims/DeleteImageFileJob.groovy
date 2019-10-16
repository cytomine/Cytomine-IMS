package ims

import be.cytomine.client.Cytomine
import be.cytomine.client.CytomineConnection
import be.cytomine.client.collections.Collection
import be.cytomine.client.models.DeleteCommand

import grails.converters.JSON

class DeleteImageFileJob {
    static triggers = {
    }

    def grailsApplication

    def execute() {
        log.info "Execute DeleteImageFile job"

        String cytomineUrl = grailsApplication.config.cytomine.ims.server.core.url
        String pubKey = grailsApplication.config.cytomine.ims.server.publicKey
        String privKey = grailsApplication.config.cytomine.ims.server.privateKey
        CytomineConnection imsConn = Cytomine.connection(cytomineUrl, pubKey, privKey, true)

        long timeMargin = grailsApplication.config.cytomine.ims.deleteJob.frequency * 1000 * 2

        //max between frequency*2 and 48h
        timeMargin = Math.max(timeMargin, 172800000L)

        Collection<DeleteCommand> commands = new Collection<DeleteCommand>(DeleteCommand.class, 0, 0)
        commands.addParams("domain", "uploadedFile")
        commands.addParams("after", (new Date().time - timeMargin).toString())
        commands = commands.fetch()
        log.info commands

        for (int i = 0; i < commands.size(); i++) {
            DeleteCommand command = (DeleteCommand) commands.list.get(i)

            def data = JSON.parse(command.get("data") as String)

            File fileToDelete = new File(data.path)
            if (fileToDelete.exists()) {
                log.info "Delete file " + fileToDelete.absolutePath
                fileToDelete.delete()
            }
        }
    }
}
