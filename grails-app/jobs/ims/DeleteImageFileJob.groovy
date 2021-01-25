package ims

import be.cytomine.client.Cytomine
import be.cytomine.client.CytomineConnection
import be.cytomine.client.collections.Collection
import be.cytomine.client.models.DeleteCommand
import be.cytomine.formats.tools.MultipleFilesFormat
import groovy.io.FileType
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.heavyconvertable.CellSensVSIFormat
import be.cytomine.exception.FormatException

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

            if(!fileToDelete.exists()) continue;

            def format
            try{
                format = FormatIdentifier.getImageFormat(fileToDelete.absolutePath)
            } catch(FormatException e) {
                if(fileToDelete.isFile()) log.error "Unknown format for file "+fileToDelete.absolutePath
                else log.info "Unknown format for "+fileToDelete.absolutePath
            }

            if(format) {
                if(!(format instanceof MultipleFilesFormat) && !(format instanceof CellSensVSIFormat)) {
                    log.info "DELETE file "+fileToDelete.absolutePath
                    fileToDelete.delete()
                } else if(format instanceof CellSensVSIFormat) {
                    if(fileToDelete.isFile() && fileToDelete.absolutePath.endsWith(".vsi")) fileToDelete = fileToDelete.parentFile

                    File vsiFile = fileToDelete.listFiles().find {it.isFile() && it.absolutePath.endsWith(".vsi")}
                    File vsiFolder =  fileToDelete.listFiles().find {it.isDirectory() && it.absolutePath.contains(vsiFile.name.replace(".vsi",""))}
                    log.info "DELETE file "+vsiFile.absolutePath
                    vsiFile.delete();
                    log.info "DELETE folder "+vsiFolder.absolutePath
                    vsiFolder.deleteDir()

                    if (fileToDelete.listFiles().size() == 0){
                        log.info "DELETE folder "+fileToDelete.absolutePath
                        fileToDelete.delete()
                    }
                } else if (format instanceof MultipleFilesFormat) {
                    if(fileToDelete.isFile()) fileToDelete = fileToDelete.parentFile
                    log.info "DELETE folder "+fileToDelete.absolutePath
                    fileToDelete.deleteDir()
                }

                //delete the broken symbolic links
                fileToDelete.parentFile.listFiles().each{ f ->
                    if(!f.exists()) {
                        log.info "DELETE not existingfile "+f.absolutePath
                        f.delete()
                    }
                }
                if (fileToDelete.parentFile.listFiles().size() == 0){
                    log.info "DELETE folder "+fileToDelete.parentFile.absolutePath
                    fileToDelete.parentFile.delete()
                }
            } else {
                log.info "DELETE file "+fileToDelete.absolutePath
                fileToDelete.delete()
                fileToDelete = fileToDelete.parentFile

                fileToDelete.eachFileRecurse (FileType.FILES) { file ->
                    try{
                        format = FormatIdentifier.getImageFormat(file.absolutePath)
                    } catch(FormatException e) {
                        log.info "DELETE file "+file.absolutePath
                        file.delete()
                    }
                }

            }
        }
    }
}
