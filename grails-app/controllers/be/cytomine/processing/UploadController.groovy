package be.cytomine.processing

import be.cytomine.client.Cytomine
import be.cytomine.client.models.AbstractImage
import be.cytomine.client.models.UploadedFile
import be.cytomine.client.models.User
import grails.converters.JSON
import org.apache.http.cookie.Cookie
import org.springframework.security.crypto.codec.Base64
import utils.FilesUtils
import utils.ProcUtils

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.HttpServletRequest


/**
 * Cytomine @ GIGA-ULG
 * User: lrollus
 * Date: 16/09/13
 * Time: 12:25
 */
class UploadController {

    def fileSystemService
    def convertImagesService
    def deployImagesService
    def imagePropertiesService
    def backgroundService
    def cytomineCoreService

    def test = {
        println "test"
        String pubKey = grailsApplication.config.grails.imageServerPublicKey
        String privKey = grailsApplication.config.grails.imageServerPrivateKey
        Cytomine cytomine = new Cytomine("http://beta.cytomine.be", pubKey, privKey, "./")
        List<Cookie> cookies = cytomine.retrieveAuthCookie("/j_spring_security_check", "j_username=johndoe&j_password=test");
        println cookies

        Cookie cook = cookies.first()

        println cook
                 //bHJvbGx1czoxMzgzMTM4NjkxMzE1OjhjNTE1NjQ4NWI0N2M0N2ZhMTUwZGZiMjRhNGU5ZGY3
                 //grails_remember_me

        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie("grails_remember_me","bHJvbGx1czoxMzgzMTM4NjkxMzE1OjhjNTE1NjQ4NWI0N2M0N2ZhMTUwZGZiMjRhNGU5ZGY3")
        cookie.setDomain("beta.cytomine.be")
        cookie.setPath("/")
        //javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie(cook.name,cook.value)

        response.addCookie(cookie)

        javax.servlet.http.Cookie cookie2 = new javax.servlet.http.Cookie(cook.name,cook.value)
        cookie2.setDomain("beta.cytomine.be")
        cookie2.setPath("/")
        cookie2.
        response.addCookie(cookie2)

        render '<img src="http://beta.cytomine.be/api/image/84215199/thumb.png" alt="out_final_52_36__overlap_..." style="max-height:260px; max-width:248px !important;">'

    }

    def upload = {

        try {

            printParamsInfo(params)
                                    //grailsApplication.config.grails.config.locations
//            String storageBufferPath = "/tmp/imageserver_buffer"
//            String cytomineUrl = "http://localhost:8080"
//            String pubKey = "a50f6f5d-1bcb-4cca-ac37-9bbf8581f25e"
//            String privKey = "278c5d52-396b-4036-b535-d541652edffa"

            String storageBufferPath = grailsApplication.config.grails.storageBufferPath
            String cytomineUrl =  params['cytomine']//grailsApplication.config.grails.cytomineUrl
            String pubKey = grailsApplication.config.grails.imageServerPublicKey
            String privKey = grailsApplication.config.grails.imageServerPrivateKey


            log.info "Upload is made on Cytomine = $cytomineUrl"
            log.info "We use $pubKey/$privKey to connect"
            log.info "Image are tmp convert in $storageBufferPath"



            def user = cytomineCoreService.tryAPIAuthentification(cytomineUrl,pubKey,privKey,request)
            log.info "user="+user.id

            def allowedMime = ["jp2", "svs", "scn", "mrxs", "ndpi", "vms", "bif"]
            def zipMime = ["zip"]
            def mimeToConvert = ["jpg", "jpeg", "png", "tiff", "tif", "pgm",  "bmp"]


            long currentUserId = user.id
            long timestamp = new Date().getTime()

            log.info "init cytomine..."
            Cytomine cytomine = new Cytomine((String) cytomineUrl, (String) user.publicKey, (String) user.privateKey, "./")

            def idStorage = Integer.parseInt(params['idStorage'] + "")
            def idProject = null
            try {
                idProject = Integer.parseInt(params['idProject'] + "")
            } catch (Exception e) {
            }
            String filename = (String) params['files[].name']
            def uploadedFilePath = new File((String) params['files[].path'])
            def size = uploadedFilePath.size()
            String contentType = params['files[].content_type']


            log.info "idStorage=$idStorage"
            log.info "idProject=$idProject"
            log.info "filename=$filename"
            log.info "uploadedFilePath=${uploadedFilePath.absolutePath}"
            log.info "size=$size"
            log.info "contentType=$contentType"

            if (!uploadedFilePath.exists()) {
                throw new Exception(uploadedFilePath.absolutePath + " NOT EXIST!")
            }
            //Move file in the buffer dir
            log.info "move file in buffer dir..."
            def newFile = moveFileTmpDir(uploadedFilePath, storageBufferPath, currentUserId, filename, timestamp)
            String extension = newFile.extension

            //Add uploadedfile on Cytomine
            String path = currentUserId + "/" + timestamp.toString() + "/" + newFile.newFilename
            log.info "create uploaded file on cytomine..."

            def uploadedFile = cytomine.addUploadedFile(
                    filename,
                    path,
                    storageBufferPath.toString(),
                    size,
                    extension,
                    contentType,
                    [idProject],
                    [idStorage],
                    currentUserId)
            log.info "uploadedFile=$uploadedFile"
            def responseContent = createResponseContent(filename, size, contentType, uploadedFile.toJSON())

            log.info "init background service..."
            backgroundService.execute("convertAndDeployImage", {
                log.info "convert file...uploadedFile=$uploadedFile"

                def uploadedFiles = convertImagesService.convertUploadedFile(cytomine, uploadedFile, currentUserId, allowedMime, mimeToConvert, zipMime)

                log.info "uploadedFiles=$uploadedFiles"

                Collection<AbstractImage> abstractImagesCreated = []
                Collection<UploadedFile> deployedFiles = []

                def storages = []
                uploadedFile.getList("storages").each {
                    log.info "get storage $it with cytomine: $cytomineUrl ${user.publicKey} ${user.privateKey}"
                    storages << cytomine.getStorage(it)
                }

                uploadedFiles.each {
                    UploadedFile new_uploadedFile = (UploadedFile) it

                    if (new_uploadedFile.getInt('status') == Cytomine.UploadStatus.TO_DEPLOY) {
                        abstractImagesCreated << deployImagesService.deployUploadedFile(cytomine, new_uploadedFile, storages)
                    }

                    if (new_uploadedFile.getInt('status') == Cytomine.UploadStatus.CONVERTED) {
                        deployImagesService.copyUploadedFile(cytomine, new_uploadedFile, storages)
                    }

                    deployedFiles << new_uploadedFile
                }

                //delete main uploaded file
                //if (!deployedFiles.contains(uploadedFile)) {
                    log.info "delete ${uploadedFile.absolutePath}"
                    deployImagesService.copyUploadedFile(cytomine, uploadedFile, storages)
                    fileSystemService.deleteFile(uploadedFile.absolutePath)
                //}
                //delete nested uploaded file
                deployedFiles.each {
                    log.info "delete local files"
                    /*def storages = []
                    it.getList("storages").each {
                        log.info "get storage $it with cytomine: $cytomineUrl ${user.publicKey} ${user.privateKey}"
                        storages << cytomine.getStorage(it)
                    }*/
                    deployImagesService.copyUploadedFile(cytomine, it, storages)
                    fileSystemService.deleteFile(it.absolutePath)
                }
                abstractImagesCreated.each { abstractImage ->
                    log.info "abstractImage=$abstractImage"
                    cytomine.clearAbstractImageProperties(abstractImage.id)
                    cytomine.populateAbstractImageProperties(abstractImage.id)
                    cytomine.extractUsefulAbstractImageProperties(abstractImage.id)
                }

            })
            def response = [responseContent]
            render response as JSON

        } catch (Exception e) {
            log.error e
            e.printStackTrace()
            response.status = 400;
            render e
            return
        }
    }



    private def printParamsInfo(def params) {
        println "params=$params"
        params.each {
            println it
            println it.class
            println "|" + it?.key + "|"
            println it?.key?.class
            println params['files[].name']
        }
    }

    private def moveFileTmpDir(def uploadedFilePath, def storageBufferPath, def currentUserId, def filename, def timestamp) {

        //compute path/filename info
        String fullDestPath = storageBufferPath + "/" + currentUserId + "/" + timestamp.toString()
        String newFilename = FilesUtils.correctFileName(filename)
        String pathFile = fullDestPath + "/" + newFilename
        String extension = FilesUtils.getExtensionFromFilename(filename).toLowerCase()

        //create dir and transfer file
        fileSystemService.makeLocalDirectory(fullDestPath)
        assert new File(fullDestPath).exists()

        println "SRC=" + uploadedFilePath.absolutePath
        println "DEST=" + new File(pathFile).absolutePath
        //uploadedFilePath.renameTo(new File(pathFile))
        def command = "mv ${uploadedFilePath.absolutePath} ${new File(pathFile).absolutePath}"
        log.info "Command=$command"
        ProcUtils.executeOnShell(command)

        println "File created: " + new File(pathFile).exists()
        return [newFilename: newFilename, extension: extension]
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
