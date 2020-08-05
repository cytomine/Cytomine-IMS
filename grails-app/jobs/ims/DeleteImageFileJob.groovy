package ims

import be.cytomine.client.Cytomine
import be.cytomine.client.collections.DeleteCommandCollection
import be.cytomine.client.models.DeleteCommand
import grails.converters.JSON
import org.codehaus.groovy.grails.web.json.JSONElement
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.supported.digitalpathology.OpenSlideMultipleFileFormat
import be.cytomine.formats.heavyconvertable.CellSensVSIFormat
import be.cytomine.exception.FormatException


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

        DeleteCommandCollection commands = cytomine.getDeleteCommandByDomainAndAfterDate("uploadedFile", (new Date().time-timeMargin))

        for(int i = 0; i<commands.size(); i++) {
            DeleteCommand command = commands.list.get(i)

            JSONElement j = JSON.parse(command.get("data"));

            File fileToDelete = new File(j.path+j.filename)

            if(!fileToDelete.exists()) continue;

            def format
            try{
                format = FormatIdentifier.getImageFormat(fileToDelete.absolutePath)
            } catch(FormatException e) {
                if(fileToDelete.isFile()) log.error "Unkown format for file "+fileToDelete.absolutePath
                else log.info "Unkown format for "+fileToDelete.absolutePath
            }

            if(format) {
                if(!(format instanceof OpenSlideMultipleFileFormat) && !(format instanceof CellSensVSIFormat)) {
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
                } else if(format instanceof OpenSlideMultipleFileFormat) {
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
            }

        }
    }
}
