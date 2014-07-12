package be.cytomine.storage

import be.cytomine.client.Cytomine
import be.cytomine.client.models.Storage
import be.cytomine.client.models.UploadedFile
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.ImageFormat
import grails.converters.JSON
import org.apache.commons.io.FilenameUtils
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.springframework.web.multipart.commons.CommonsMultipartFile
import utils.FilesUtils

/**
 * Cytomine @ GIGA-ULG
 * User: lrollus
 * Date: 16/09/13
 * Time: 12:25
 */
@RestApi(name = "upload services", description = "Methods for uploading images")
class StorageController {

    def deployImagesService
    def backgroundService
    def cytomineService

    @RestApiMethod(description="Method for uploading an image")
    @RestApiParams(params=[
    @RestApiParam(name="files[]", type="data", paramType = RestApiParamType.QUERY, description = "The files content (Multipart)"),
    @RestApiParam(name="cytomine", type="String", paramType = RestApiParamType.QUERY, description = "The url of Cytomine"),
    @RestApiParam(name="idStorage", type="int", paramType = RestApiParamType.QUERY, description = "The id of the targeted storage"),
    @RestApiParam(name="sync", type="boolean", paramType = RestApiParamType.QUERY, description = "Indicates if operations are done synchronously or not (false by default)", required = false),
    @RestApiParam(name="idProject", type="int", paramType = RestApiParamType.QUERY, description = " The id of the targeted project", required = false),
    @RestApiParam(name="keys", type="String", paramType = RestApiParamType.QUERY, description = "The keys of the properties you want to link with your files (e.g. : key1,key2, ...)", required = false),
    @RestApiParam(name="keys", type="String", paramType = RestApiParamType.QUERY, description = "The values of the properties you want to link with your files (e.g. : key1,key2, ...)", required = false)
    ])
    def upload () {

        try {

            String storageBufferPath = grailsApplication.config.cytomine.storageBufferPath
            String cytomineUrl =  params['cytomine']//grailsApplication.config.grails.cytomineUrl
            String pubKey = grailsApplication.config.cytomine.imageServerPublicKey
            String privKey = grailsApplication.config.cytomine.imageServerPrivateKey

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
            String destPath = File.separator + timestamp.toString() + File.separator + FilesUtils.correctFileName(filename)

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
                    currentUserId)
            deployImagesService.copyUploadedFile(cytomine, uploadedFilePath.absolutePath, uploadedFile, [storage])


            String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join("")

            def imageFormats = FormatIdentifier.getImageFormats(
                    originalFilenameFullPath
            )

            println "imageFormats = $imageFormats"

            if (imageFormats.size() == 0) { //not a file that we can recognize
                //todo: response error message
                return
            }

            def images = []
            if(isSync) {
                log.info "Execute convert & deploy NOT in background (sync=true!)"
                cytomine.editUploadedFile(uploadedFile.id, 6) //to deploy
                def imageFormatsToDeploy = convertImage(imageFormats,storageBufferPath)
                uploadedFile = cytomine.editUploadedFile(uploadedFile.id, 1)
                imageFormatsToDeploy.each {
                    images << createImage(cytomine,it,filename,storage,contentType,projects,idStorage,currentUserId,properties, uploadedFile)
                }
                cytomine.editUploadedFile(uploadedFile.id, 2) //deployed
                log.info "image sync = $images"
            } else {
                log.info "Execute convert & deploy into background"
                backgroundService.execute("convertAndDeployImage", {
                    cytomine.editUploadedFile(uploadedFile.id, 6) //to deploy
                    def imageFormatsToDeploy = convertImage(imageFormats,storageBufferPath)
                    uploadedFile = cytomine.editUploadedFile(uploadedFile.id, 1)
                    imageFormatsToDeploy.each {
                        images << createImage(cytomine,it,filename,storage,contentType,projects,idStorage,currentUserId,properties, uploadedFile)
                    }
                    cytomine.editUploadedFile(uploadedFile.id, 2) //deployed
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

    //todo : move into service
    private def convertImage(def filesToDeploy,String storageBufferPath) {
        //start to convert into pyramid format, if necessary
        def imageFormatsToDeploy = []
        filesToDeploy.each { fileToDeploy ->
            ImageFormat imageFormat = fileToDeploy.imageFormat
            String convertedImageFilename = imageFormat.convert(storageBufferPath)
            if (!convertedImageFilename) { //not necessary to convert it
                fileToDeploy.parent = fileToDeploy
                imageFormatsToDeploy << fileToDeploy
            } else {
                FormatIdentifier.getImageFormats(convertedImageFilename).each { convertedImageFormat ->
                    convertedImageFormat.parent = fileToDeploy
                    imageFormatsToDeploy << convertedImageFormat
                }
            }
        }
        return imageFormatsToDeploy
    }

    //todo : move into service
    private def createImage(Cytomine cytomine, def imageFormatsToDeploy, String filename, Storage storage,def contentType, List projects, long idStorage, long currentUserId, def properties, UploadedFile uploadedFile) {
        println "createImage $imageFormatsToDeploy"

        ImageFormat imageFormat = imageFormatsToDeploy.imageFormat
        ImageFormat parentImageFormat = imageFormatsToDeploy.parent?.imageFormat

        File f = new File(imageFormat.absoluteFilePath)
        def parentUploadedFile = null
        if (parentImageFormat &&  imageFormatsToDeploy.parent?.uploadedFilePath != parentImageFormat.absoluteFilePath) {
            parentUploadedFile = cytomine.addUploadedFile(
                    (String) filename,
                    (String) ((String)parentImageFormat.absoluteFilePath).replace(storage.getStr("basePath"), ""),
                    (String) storage.getStr("basePath"),
                    f.size(),
                    (String) FilesUtils.getExtensionFromFilename(parentImageFormat.absoluteFilePath).toLowerCase(),
                    (String) contentType,
                    (String) parentImageFormat.mimeType,
                    projects,
                    [idStorage],
                    currentUserId,
                    -1l,
                    uploadedFile.id,
                    null)
        } else {
            //put correct mime_type in uploadedFile
            uploadedFile.set('mimeType', parentImageFormat.mimeType)
            cytomine.updateModel(uploadedFile)
        }

        UploadedFile finalParent = (parentUploadedFile) == null ? uploadedFile : parentUploadedFile
        String originalFilename =  (parentUploadedFile) == null ? filename :  FilenameUtils.getName(parentUploadedFile.absolutePath)

        def _uploadedFile = cytomine.addUploadedFile(
                (String) originalFilename,
                (String) ((String)imageFormat.absoluteFilePath).replace(storage.getStr("basePath"), ""),
                (String) storage.getStr("basePath"),
                f.size(),
                (String) FilesUtils.getExtensionFromFilename(imageFormat.absoluteFilePath).toLowerCase(),
                (String) contentType,
                (String) imageFormat.mimeType,
                projects,
                [idStorage],
                currentUserId,
                -1l,
                finalParent.id,
                uploadedFile.id)

        def image = cytomine.addNewImage(_uploadedFile.id)

        properties.each {
            cytomine.addDomainProperties(image.getStr("class"),image.getLong("id"),it.key.toString(),it.value.toString())
        }
        return image
    }

    //todo : move into service
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
