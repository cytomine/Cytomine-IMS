package be.cytomine.processing

import be.cytomine.client.Cytomine
import be.cytomine.client.models.AbstractImage
import be.cytomine.client.models.UploadedFile
import be.cytomine.client.models.User
import grails.converters.JSON
import org.springframework.security.crypto.codec.Base64
import utils.FilesUtils

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

    def test = {
        println "test"

        if (tryAPIAuthentification(request)) {
            println "CYTOMINE LOGGIN OK"
        } else {
            println "CYTOMINE LOGGIN NOK"
        }
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



            def user = tryAPIAuthentification(cytomineUrl,pubKey,privKey,request)
            log.info "user="+user.id

            def allowedMime = ["svs", "opt", "jp2", "scn"]
            def zipMime = ["zip"]
            def mimeToConvert = ["jpg", "jpeg", "png", "tiff", "tif", "pgm"]//, "ndpi"]


            def currentUserId = user.id
            long timestamp = new Date().getTime()

            log.info "init cytomine..."
            Cytomine cytomine = new Cytomine(cytomineUrl, user.publicKey, user.privateKey, "./")

            def idStorage = Integer.parseInt(params['idStorage'] + "")
            def idProject = null
            try {
                idProject = Integer.parseInt(params['idProject'] + "")
            } catch (Exception e) {
            }
            def filename = params['files[].name']
            def uploadedFilePath = new File(params['files[].path'])
            def size = uploadedFilePath.size()
            def contentType = params['files[].content_type']


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

            //Add uploadedfile on Cytomine
            def path = currentUserId + "/" + timestamp.toString() + "/" + newFile.newFilename
            log.info "create uploaded file on cytomine..."
            def uploadedFile = cytomine.addUploadedFile(filename, path, storageBufferPath.toString(), size, newFile.extension, contentType, [idProject], [idStorage], currentUserId)
            log.info "uploadedFile=$uploadedFile"
            def responseContent = createResponseContent(filename, size, contentType, uploadedFile.toJSON())

            log.info "init background service..."
            backgroundService.execute("convertAndDeployImage", {
                log.info "convert file..."
                def uploadedFiles = convertImagesService.convertUploadedFile(cytomine, uploadedFile, currentUserId, allowedMime, mimeToConvert, zipMime)


                Collection<AbstractImage> abstractImagesCreated = []
                Collection<UploadedFile> deployedFiles = []

                uploadedFiles.each {
                    UploadedFile new_uploadedFile = (UploadedFile) it


                    def storages = []
                    uploadedFile.getList("storages").each {
                        storages << cytomine.getStorage(it)
                    }


                    if (new_uploadedFile.getInt('status') == Cytomine.UploadStatus.TO_DEPLOY) {
                        abstractImagesCreated << deployImagesService.deployUploadedFile(cytomine, new_uploadedFile, storages)
                    }

                    if (new_uploadedFile.getInt('status') == Cytomine.UploadStatus.CONVERTED) {
                        deployImagesService.copyUploadedFile(cytomine, new_uploadedFile, storages)
                    }

                    deployedFiles << new_uploadedFile
                }

                //delete main uploaded file
                if (!deployedFiles.contains(uploadedFile)) {
                    log.info "delete ${uploadedFile.absolutePath}"
                    fileSystemService.deleteFile(uploadedFile.absolutePath)
                }
                //delete nested uploaded file
                deployedFiles.each {
                    log.info "delete local files"
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
        uploadedFilePath.renameTo(new File(pathFile))
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

    private def tryAPIAuthentification(def cytomineUrl,def ISPubKey, def ISPrivKey, HttpServletRequest request) {
        println "tryAPIAuthentification"
        String authorization = request.getHeader("authorization")
        println "authorization=$authorization"
        if (request.getHeader("dateFull") == null && request.getHeader("date") == null) {
            throw new Exception("Auth failed: no date")
        }
        if (request.getHeader("host") == null) {
            throw new Exception("Auth failed: no host")
        }
        if (authorization == null) {
            throw new Exception("Auth failed: no authorization")
        }
        if (!authorization.startsWith("CYTOMINE") || !authorization.indexOf(" ") == -1 || !authorization.indexOf(":") == -1) {
            throw new Exception("Auth failed: bad authorization")
        }
        request.getHeaderNames().each {
            println it + "=" + request.getHeader(it)
        }


        String content_md5 = (request.getHeader("content-MD5") != null) ? request.getHeader("content-MD5") : ""
        //println "content_md5=" + content_md5
        String content_type = (request.getHeader("content-type") != null) ? request.getHeader("content-type") : ""
        content_type = (request.getHeader("Content-Type") != null) ? request.getHeader("Content-Type") : content_type
        content_type = (request.getHeader("content-type-full") != null) ? request.getHeader("content-type-full") : content_type


        //println "content_type=" + content_type
        String date = (request.getHeader("date") != null) ? request.getHeader("date") : ""
        date = (request.getHeader("dateFull") != null) ? request.getHeader("dateFull") : date

        //println "date=" + date
        String canonicalHeaders = request.getMethod() + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n"
        //println "canonicalHeaders=" + canonicalHeaders
        String canonicalExtensionHeaders = ""
        String queryString = (request.getQueryString() != null) ? "?" + request.getQueryString() : ""
        String path = request.forwardURI //original URI Request
        String canonicalResource = path + queryString
        //println "canonicalResource=" + canonicalResource
        String messageToSign = canonicalHeaders + canonicalExtensionHeaders + canonicalResource
        //println "messageToSign=$messageToSign"
        String accessKey = authorization.substring(authorization.indexOf(" ") + 1, authorization.indexOf(":"))
        //println "accessKey=" + accessKey
        String authorizationSign = authorization.substring(authorization.indexOf(":") + 1)
        //println "authorizationSign=" + authorizationSign
        //TODO: ask user with public key
        //TODO: pubKey = a50f6f5d-1bcb-4cca-ac37-9bbf8581f25e, privKey = 278c5d52-396b-4036-b535-d541652edffa

        log.info "content_md5=$content_md5"
        log.info "content_type=$content_type"
        log.info "date=$date"
        log.info "queryString=$queryString"
        log.info "path=$path"
        log.info "method=${request.getMethod()}"

        println "accessKey=$accessKey"
        Cytomine cytomine = new Cytomine(cytomineUrl, ISPubKey,ISPrivKey, "./")
        User user = cytomine.getKeys(accessKey)
        long id = cytomine.getUser(accessKey).id
        if (!user) {
            println "User not found with key $accessKey!"
            throw new Exception("Auth failed: User not found with key $accessKey!")
        }

        //TODO: get its private key
        String key = user.get("privateKey")


        println "Privatekey=${user.get("privateKey")}"
        println "PublicKey=${user.get("publicKey")}"

        SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1")
        //println "signingKey=" + signingKey
        // get an hmac_sha1 Mac instance and initialize with the signing key
        Mac mac = Mac.getInstance("HmacSHA1")
        //println "mac=" + mac
        mac.init(signingKey)
        // compute the hmac on input data bytes
        byte[] rawHmac = mac.doFinal(new String(messageToSign.getBytes(), "UTF-8").getBytes())
        //println "rawHmac=" + rawHmac.length
        // base64-encode the hmac
        byte[] signatureBytes = Base64.encode(rawHmac)
        //println "signatureBytes=" + signatureBytes.length
        def signature = new String(signatureBytes)
        //println "signature=" + signature

        println "authorizationSign=$authorizationSign"
        println "signature=$signature"
        if (authorizationSign == signature) {
            println "AUTH TRUE"
            return ["id": id, "privateKey": user.get("privateKey"), "publicKey": user.get("publicKey")]
        } else {
            println "AUTH FALSE"
            throw new Exception("Auth failed")
        }
    }

}
