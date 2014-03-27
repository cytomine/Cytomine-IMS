package cytomine.web

import be.cytomine.client.Cytomine
import be.cytomine.client.models.AbstractImage
import be.cytomine.client.models.Storage
import be.cytomine.client.models.UploadedFile
import utils.ProcUtils

/**
 * TODOSTEVEBEN: Doc + refactoring + security?
 */
class DeployImagesService {

    def fileSystemService

    static transactional = true

    UploadedFile copyUploadedFile(Cytomine cytomine, UploadedFile uploadedFile, Collection<Storage> storages) {
        def localFile = uploadedFile.get("path") + "/" + uploadedFile.get("filename")

        storages.each { storage ->
            def destFilename = storage.getStr("basePath") + "/" + uploadedFile.getStr("filename")
            fileSystemService.makeLocalDirectory(new File(destFilename).parent)

            def command = """mv "$localFile" "$destFilename" """
            log.info "Command=$command"
            ProcUtils.executeOnShell(command)

            if(!new File(destFilename).exists()) {
                log.error new File(destFilename).absolutePath + " created = " + new File(destFilename).exists()
                throw new Exception(new File(destFilename).absolutePath + " is not created! ")
            }

        }
        uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.DEPLOYED)
        return uploadedFile
    }


    AbstractImage deployUploadedFile(Cytomine cytomine,UploadedFile uploadedFile,  Collection<Storage> storages) {
        log.info "deployUploadedFile"
        //copy it
        uploadedFile = copyUploadedFile(cytomine,uploadedFile, storages)
        log.info "###############################################"
        log.info "############ADD IMAGE################"
        log.info "###############################################"
        AbstractImage abstractImage = cytomine.addNewImage(uploadedFile.id)

        return abstractImage
    }
}
