package cytomine.web

import be.cytomine.client.Cytomine
import be.cytomine.client.models.Storage
import be.cytomine.client.models.UploadedFile
import utils.ProcUtils

/**
 * TODOSTEVEBEN: Doc + refactoring + security?
 */
class DeployImagesService {

    def fileSystemService

    static transactional = true

    //could be removed and use an standard MV function by passing two directories...
    UploadedFile copyUploadedFile(Cytomine cytomine, String uploadedFilePath, uploadedFile, Collection<Storage> storages) {
        def localFile = uploadedFilePath

        storages.each { storage ->
            def destFilename = storage.getStr("basePath") + File.separator + uploadedFile.getStr("filename")
            if(!new File(new File(destFilename).parent).exists()) {
                fileSystemService.makeLocalDirectory(new File(destFilename).parent)
            }

            def command = """mv "$localFile" "$destFilename" """
            log.info "Command=$command"
            ProcUtils.executeOnShell(command)

            if(!new File(destFilename).exists()) {
                log.error new File(destFilename).absolutePath + " created = " + new File(destFilename).exists()
                throw new Exception(new File(destFilename).absolutePath + " is not created! ")
            }

        }
        return uploadedFile
    }
}
