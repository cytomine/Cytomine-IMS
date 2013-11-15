package cytomine.web

import be.cytomine.client.Cytomine
import be.cytomine.client.models.AbstractImage
import be.cytomine.client.models.Storage
import be.cytomine.client.models.UploadedFile
import grails.converters.JSON
import utils.ProcUtils

import javax.activation.MimetypesFileTypeMap

/**
 * TODOSTEVEBEN: Doc + refactoring + security?
 */
class DeployImagesService {

    def remoteCopyService
    def cytomineService
    def imageInstanceService
    def storageAbstractImageService
    def projectService
    def fileSystemService

    static transactional = true

    UploadedFile copyUploadedFile(Cytomine cytomine,UploadedFile uploadedFile, Collection<Storage> storages) {
        def localFile = uploadedFile.get("path") + "/" + uploadedFile.get("filename")

        storages.each { storage ->
                def remoteFile = storage.getStr("basePath") + "/" + uploadedFile.getStr("filename")
            log.info "basePath= " + storage.getStr("basePath")
            log.info "filename= " + uploadedFile.getStr("filename")
//                remoteCopyService.copy(localFile, remotePath, remoteFile, storage, true)
            log.info "LOCAL FILE = " + localFile
            log.info "REMOTE FILE = " + remoteFile

            fileSystemService.makeLocalDirectory(new File(remoteFile).parent)

            def command = "mv $localFile $remoteFile"
            log.info "Command=$command"
            ProcUtils.executeOnShell(command)

                if(!new File(remoteFile).exists()) {
                    log.error new File(remoteFile).absolutePath + " created = " + new File(remoteFile).exists()
                    throw new Exception(new File(remoteFile).absolutePath + " is not created! ")
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
