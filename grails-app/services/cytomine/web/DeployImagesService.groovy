package cytomine.web

import be.cytomine.client.Cytomine
import be.cytomine.client.models.UploadedFile
import be.cytomine.image.server.Storage
import be.cytomine.laboratory.Sample
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import grails.converters.JSON
import org.codehaus.groovy.grails.plugins.springsecurity.SpringSecurityUtils

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

    static transactional = true

    UploadedFile copyUploadedFile(Cytomine cytomine,UploadedFile uploadedFile, Collection<Storage> storages) {
        def localFile = uploadedFile.get("path") + "/" + uploadedFile.get("filename")

        storages.each { storage ->
                def remoteFile = storage.getStr("basePath") + "/" + uploadedFile.getStr("filename")
                log.info "REMOTE FILE = " + remoteFile

                remoteCopyService.copy(localFile, remotePath, remoteFile, storage, true)

                new File(localFile).renameTo(new File(remoteFile))

                if(!new File(remoteFile).exists()) {
                    log.error new File(remoteFile).absolutePath + " created = " + new File(remoteFile).exists()
                    throw new Exception(new File(remoteFile).absolutePath + " is not created! ")
                }

        }
        uploadedFile = cytomine.editUploadedFile(uploadedFile.id,cytomine.UploadStatus.DEPLOYED)
        return uploadedFile
    }


    AbstractImage deployUploadedFile(Cytomine cytomine,UploadedFile uploadedFile,  Collection<Storage> storages) {

//        SpringSecurityUtils.reauthenticate currentUser.getUsername(), null
//        uploadedFile.refresh()

        //copy it
        uploadedFile = copyUploadedFile(cytomine,uploadedFile, storages)

        AbstractImage abstractImage = cytomine.addNewImage(uploadedFile)

        return abstractImage
    }
}
