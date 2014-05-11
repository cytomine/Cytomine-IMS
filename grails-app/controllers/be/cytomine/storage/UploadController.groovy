package be.cytomine.storage

import be.cytomine.client.Cytomine
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.ImageFormat
import grails.converters.JSON
import utils.FilesUtils

/**
 * Cytomine @ GIGA-ULG
 * User: lrollus
 * Date: 16/09/13
 * Time: 12:25
 */
class UploadController {

    def convertImagesService
    def deployImagesService
    def backgroundService
    def cytomineService

    def upload () {

        try {

            String storageBufferPath = grailsApplication.config.grails.storageBufferPath
            String cytomineUrl =  params['cytomine']//grailsApplication.config.grails.cytomineUrl
            String pubKey = grailsApplication.config.grails.imageServerPublicKey
            String privKey = grailsApplication.config.grails.imageServerPrivateKey

            log.info "Upload is made on Cytomine = $cytomineUrl"
            log.info "We use $pubKey/$privKey to connect"
            log.info "Image are tmp convert in $storageBufferPath"

            def user = cytomineService.tryAPIAuthentification(cytomineUrl,pubKey,privKey,request)
            long currentUserId = user.id
            long timestamp = new Date().getTime()

            log.info "init cytomine..."
            Cytomine cytomine = new Cytomine((String) cytomineUrl, (String) user.publicKey, (String) user.privateKey, "./")

            def idStorage = Integer.parseInt(params['idStorage'] + "")
            def projects = []
            if (params['idProject']) {
                try {
                    projects << Integer.parseInt(params['idProject'] + "")
                } catch (Exception e) {
                }
            }

            String filename = (String) params['files[].name']
            def uploadedFilePath = new File((String) params['files[].path'])
            def size = uploadedFilePath.size()
            String contentType = params['files[].content_type']

            log.info "idStorage=$idStorage"
            log.info "projects=$projects"
            log.info "filename=$filename"
            log.info "absoluteFilePath=${uploadedFilePath.absolutePath}"
            log.info "size=$size"
            log.info "contentType=$contentType"

            if (!uploadedFilePath.exists()) {
                throw new Exception(uploadedFilePath.absolutePath + " NOT EXIST!")
            }

            log.info "Create an uploadedFile instance and copy it to its storages"
            String extension = FilesUtils.getExtensionFromFilename(filename).toLowerCase()
            String destPath = timestamp.toString() + "/" + FilesUtils.correctFileName(filename)


            def storage = cytomine.getStorage(idStorage)
            def uploadedFile = cytomine.addUploadedFile(
                    filename,
                    destPath,
                    storage.getStr("basePath"),
                    size,
                    extension,
                    contentType,
                    projects,
                    [idStorage],
                    currentUserId)
            deployImagesService.copyUploadedFile(cytomine, uploadedFilePath.absolutePath, uploadedFile, [storage])



            log.info "Execute convert & deploy into background"
            backgroundService.execute("convertAndDeployImage", {
                String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)

                ImageFormat[] imageFormats = FormatIdentifier.getImageFormats(
                        originalFilenameFullPath
                )

                println "imageFormats $imageFormats"
                if (imageFormats.size() == 0) { //not a file that we can recognize
                    //todo: response error message
                    return
                }

                //start to convert into pyramid format, if necessary
                def imageFormatsToDeploy = []
                imageFormats.each { imageFormat ->
                    String convertedImageFilename = imageFormat.convert(storageBufferPath)
                    if (!convertedImageFilename) { //not necessary to convert it
                        imageFormatsToDeploy << imageFormat
                    } else {
                        FormatIdentifier.getImageFormats(convertedImageFilename).each { convertedImageFormat ->
                            imageFormatsToDeploy << convertedImageFormat
                        }
                    }
                }

                println "convertedImageFormats $imageFormatsToDeploy"
                imageFormatsToDeploy.each {

                    File f = new File(it.absoluteFilePath)
                    def _uploadedFile = cytomine.addUploadedFile(
                            filename,
                            ((String)it.absoluteFilePath).replace(storage.getStr("basePath"), ""),
                            storage.getStr("basePath"),
                            f.size(),
                            FilesUtils.getExtensionFromFilename(it.absoluteFilePath).toLowerCase(),
                            contentType,
                            projects,
                            [idStorage],
                            currentUserId)
                    cytomine.addNewImage(_uploadedFile.id)

                }
            })

            def responseContent = [createResponseContent(filename, size, contentType, uploadedFile.toJSON())]
            render responseContent as JSON

        } catch (Exception e) {
            log.error e
            e.printStackTrace()
            response.status = 400;
            render e
            return
        }
    }



    private def createResponseContent(def filename, def size, def contentType, def uploadedFileJSON) {
        def content = [:]
        content.status = 200;
        content.name = filename
        content.size = size
        content.type = contentType
        content.uploadFile = uploadedFileJSON
        return content
    }



}
