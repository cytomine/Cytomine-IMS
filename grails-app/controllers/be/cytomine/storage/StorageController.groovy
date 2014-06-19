package be.cytomine.storage

import be.cytomine.client.Cytomine
import be.cytomine.client.models.Storage
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
class StorageController {

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

            def properties = [:]
            def keys = []
            def values = []
            log.info "keys=" + params["keys"]
            log.info "values=" + params["values"]
            if(params["keys"]!=null && params["keys"]!="") {
                keys = params["keys"].split(",")
                values = params["values"].split(",")
            }
            if(keys.size()!=values.size()) {
                throw new Exception("Key.size <> Value.size!");
            }
            keys.eachWithIndex { key, index ->
                properties[key]=values[index];
            }

            boolean isSync = params.boolean('sync')
            log.info "sync="+isSync

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
            log.info "storage.getStr(basePath) : " + storage.getStr("basePath")
            def uploadedFile = cytomine.addUploadedFile(
                    filename,
                    destPath,
                    storage.getStr("basePath"),
                    size,
                    extension,
                    contentType,
                    null,
                    projects,
                    [idStorage],
                    currentUserId,
                    null)
            deployImagesService.copyUploadedFile(cytomine, uploadedFilePath.absolutePath, uploadedFile, [storage])


            String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)

            ImageFormat[] imageFormats = FormatIdentifier.getImageFormats(
                    originalFilenameFullPath
            )

            println "imageFormats $imageFormats"
            if (imageFormats.size() == 0) { //not a file that we can recognize
                //todo: response error message
                return
            }

            def images = []
            if(isSync) {
                log.info "Execute convert & deploy NOT in background (sync=true!)"
                def imageFormatsToDeploy = convertImage(imageFormats,storageBufferPath)
                images = createImage(cytomine,imageFormatsToDeploy,filename,storage,contentType,projects,idStorage,currentUserId,properties, uploadedFile.id)
                log.info "image sync = $images"
            } else {
                log.info "Execute convert & deploy into background"
                backgroundService.execute("convertAndDeployImage", {
                    def imageFormatsToDeploy = convertImage(imageFormats,storageBufferPath)
                    images = createImage(cytomine,imageFormatsToDeploy,filename,storage,contentType,projects,idStorage,currentUserId,properties, uploadedFile.id)
                    log.info "image async = $images"
                })
            }

            def responseContent = [createResponseContent(filename, size, contentType, uploadedFile.toJSON(),images)]
            render responseContent as JSON



        } catch (Exception e) {
            log.error e
            e.printStackTrace()
            response.status = 400;
            render e
            return
        }
    }

    private def convertImage(ImageFormat[] imageFormats,String storageBufferPath) {
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
        return imageFormatsToDeploy
    }


    private def createImage(Cytomine cytomine, def imageFormatsToDeploy, String filename, Storage storage,def contentType, def projects, def idStorage, def currentUserId, def properties, long idParent) {
        println "convertedImageFormats $imageFormatsToDeploy"
        def images = []
        imageFormatsToDeploy.each {

            File f = new File(it.absoluteFilePath)

            def _uploadedFile = cytomine.addUploadedFile(
                    filename,
                    ((String)it.absoluteFilePath).replace(storage.getStr("basePath"), ""),
                    storage.getStr("basePath"),
                    f.size(),
                    FilesUtils.getExtensionFromFilename(it.absoluteFilePath).toLowerCase(),
                    contentType,
                    it.mimeType,
                    projects,
                    [idStorage],
                    currentUserId,
                    -1l,
                    idParent)
            def image = cytomine.addNewImage(_uploadedFile.id)

            properties.each {
                println it.key
                println it.value
                cytomine.addDomainProperties(image.getStr("class"),image.getLong("id"),it.key.toString(),it.value.toString())
            }

            images << image

        }
        return images
    }


    private def responseFile(File file) {
        response.setHeader "Content-disposition", "attachment; filename=\"${file.getName()}\"";
        response.outputStream << file.newInputStream();
        response.outputStream.flush();
    }

    def download() {
        String fif = params.get("fif")
        responseFile(new File(fif))
    }


    private def createResponseContent(def filename, def size, def contentType, def uploadedFileJSON, def images) {
        def content = [:]
        content.status = 200;
        content.name = filename
        content.size = size
        content.type = contentType
        content.uploadFile = uploadedFileJSON
        content.images = images
        return content
    }



}
