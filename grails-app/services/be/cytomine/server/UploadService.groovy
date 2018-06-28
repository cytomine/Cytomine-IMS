package be.cytomine.server

/*
 * Copyright (c) 2009-2017. Authors: see NOTICE file.
 *
 * Licensed under the GNU Lesser General Public License, Version 2.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import be.cytomine.client.Cytomine
import be.cytomine.client.collections.ImageInstanceCollection
import be.cytomine.client.models.Annotation
import be.cytomine.client.models.ImageGroup
import be.cytomine.client.models.ImageSequence
import be.cytomine.client.models.Property
import be.cytomine.client.models.Storage
import be.cytomine.client.models.UploadedFile
import be.cytomine.exception.FormatException
import be.cytomine.formats.archive.ArchiveFormat
import be.cytomine.formats.Format
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.heavyconvertable.BioFormatConvertable
import be.cytomine.formats.heavyconvertable.IHeavyConvertableImageFormat
import be.cytomine.formats.lightconvertable.ILightConvertableImageFormat
import be.cytomine.formats.lightconvertable.VIPSConvertable
import grails.util.Holders
import utils.FilesUtils

import java.util.concurrent.Callable

class UploadService {

    def backgroundService
    def deployImagesService
    def grailsApplication

    // WARNING ! This function is recursive. Be careful !
    def upload(Cytomine cytomine, String filename, Long idStorage, String contentType, def filePath,
               def projects, long currentUserId, def properties, long timestamp, boolean isSync, Long idParent = null) {

        def uploadedFilePath = new File(filePath)
        log.info "filePath=$filePath"

        log.info "absoluteFilePath=${uploadedFilePath.absolutePath}"
        if (!uploadedFilePath.exists()) {
            throw new FileNotFoundException(uploadedFilePath.absolutePath + " NOT EXIST!")
        }
        def size = uploadedFilePath.size()
        log.info "size=$size"

        log.info "Create an uploadedFile instance and copy it to its storages"
        String extension = FilesUtils.getExtensionFromFilename(filename).toLowerCase()
        String destPath = File.separator + timestamp.toString() + File.separator + FilesUtils.correctFileName(filename)

        def storage = cytomine.getStorage(idStorage)
        log.info "storage.getStr(basePath) : " + storage.getStr("basePath")
        // no parent
        def uploadedFile = cytomine.addUploadedFile(
                filename,
                destPath,
                storage.getStr("basePath"),
                size,
                extension,
                contentType,
                projects,
                [idStorage],
                currentUserId,
                0, // UPLOADED status
                idParent
        )

        deployImagesService.copyUploadedFile(cytomine, uploadedFilePath.absolutePath, uploadedFile, [storage])


        String storageBufferPath = Holders.config.cytomine.storageBufferPath
        log.info "Image are tmp convert in $storageBufferPath"
        String originalFilenameFullPath = [uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join("")


        def imageFormats
        try {
            imageFormats = FormatIdentifier.getImageFormats(originalFilenameFullPath)
        } catch (FormatException e) {
            log.warn "Undetected format"
            log.warn e.toString()
            uploadedFile = cytomine.editUploadedFile(uploadedFile.id, 3) // status ERROR FORMAT
            return
        }

        log.info "imageFormats = $imageFormats"
        if (imageFormats.size() == 0) { //not a file that we can recognize
            //todo: response error message
            uploadedFile = cytomine.editUploadedFile(uploadedFile.id, 3) // status ERROR FORMAT
            return
        }


        def heavyConvertableImageFormats = imageFormats.findAll {
            it.imageFormat == null || it.imageFormat instanceof IHeavyConvertableImageFormat
        }
        imageFormats = imageFormats - heavyConvertableImageFormats

        def images = []

        def convertAndCreate = {
            cytomine.editUploadedFile(uploadedFile.id, 6) //to deploy
            imageFormats.each {
                images << createImage(cytomine, it, filename, storage, contentType, projects, idStorage,
                                      currentUserId, properties, uploadedFile)
            }
            cytomine.editUploadedFile(uploadedFile.id, 2) //deployed
        }

        def conversion = { image ->

            def files = image.imageFormat.convert()

            def tmpResponseContent = []
            files.each { file ->
                def path = file.path
                def nameNewFile = path.substring(path.lastIndexOf("/") + 1)
                // maybe redetermine the contentType ?
                // recursion
                def tmp = upload(cytomine, nameNewFile, idStorage, contentType, path, projects,
                                 currentUserId, properties, timestamp, true, uploadedFile.id)[0]
                tmp.z = file.z
                tmp.t = file.t
                tmp.c = file.c
                tmpResponseContent << tmp

            }

            if (image.imageFormat instanceof BioFormatConvertable && image.imageFormat.group) {
                def newFiles = []
                tmpResponseContent.each { tmp ->
                    def newFile = [:]
                    def outputImage = tmp.images[0]
                    newFile.path = outputImage.get("filename")
                    newFile.id = outputImage.id
                    newFile.z = tmp.z
                    newFile.t = tmp.t
                    newFile.c = tmp.c
                    newFiles << newFile
                }

                projects.each { project ->
                    groupImages(cytomine, newFiles, project)
                }
            }

            int status = tmpResponseContent.size() > 0 && tmpResponseContent?.status.unique() == [200] ? 1 : 4
            cytomine.editUploadedFile(uploadedFile.id, status) // converted or error conversion
        }

        if (isSync) {
            log.info "Execute convert & deploy NOT in background (sync=true!)"
            convertAndCreate()
            heavyConvertableImageFormats.each {
                log.info "unsupported image " + it
                conversion(it)
            }
            log.info "image sync = $images"
        }
        else {
            log.info "Execute convert & deploy into background"
            backgroundService.execute("convertAndDeployImage", {
                convertAndCreate()
                heavyConvertableImageFormats.each {
                    log.info "unsupported image " + it
                    conversion(it)
                }
                log.info "image async = $images"
            })
        }

        def responseContent = [createResponseContent(filename, size, contentType, uploadedFile.toJSON(), images)]
        return responseContent
    }

    private def createImage(Cytomine cytomine,
                            def imageFormatsToDeploy, String filename, Storage storage,
                            def contentType, List projects, long idStorage, long currentUserId,
                            def properties, UploadedFile uploadedFile) {
        def annotations = []
        log.info "createImage $imageFormatsToDeploy"

        Format imageFormat = imageFormatsToDeploy.imageFormat
        String originalName = new File(imageFormat.absoluteFilePath).name

        Format parentImageFormat = imageFormatsToDeploy.parent?.imageFormat

        // HACK to get properties from original file such as DICOM
        if (imageFormat instanceof VIPSConvertable) {
            log.info "prop"
            log.info imageFormat.properties()
            properties += imageFormat.properties().collectEntries() {[(it.key):it.value]}

            log.info "annotations"
            log.info imageFormat.annotations()
            annotations += imageFormat.annotations()
        }

        if (imageFormat instanceof ILightConvertableImageFormat) {
            String newImage = imageFormat.convert()
            imageFormat = FormatIdentifier.getImageFormat(newImage)
            imageFormatsToDeploy = [uploadedFilePath: newImage, imageFormat: imageFormat]
        }

        // TODO find a way to unify absoluteFilePath & uploadedFilepath
        String path = imageFormatsToDeploy.uploadedFilePath ?: imageFormatsToDeploy.absoluteFilePath
        String shortPath = path.replace(storage.getStr("basePath"), "")

        def childUploadedFile = null
        if (parentImageFormat instanceof ArchiveFormat) {
            File f = new File(imageFormat.absoluteFilePath)
            contentType = imageFormatsToDeploy.imageFormat.mimeType ?: URLConnection.guessContentTypeFromName(f.getName())

            childUploadedFile = cytomine.addUploadedFile(
                    originalName,
                    (String) shortPath,
                    (String) storage.getStr("basePath"),
                    f.size(),
                    (String) FilesUtils.getExtensionFromFilename(path).toLowerCase(),
                    contentType,
                    projects,
                    [idStorage],
                    currentUserId,
                    2, //DEPLOYED status
                    uploadedFile.id, // this is the parent
            )

        }

        UploadedFile finalParent = childUploadedFile ?: uploadedFile

        def image = cytomine.addNewImage(finalParent.id, shortPath, finalParent.get('filename') as String,
                                         imageFormatsToDeploy.imageFormat.mimeType)

        log.info "properties"
        def props = []
        def property
        properties.each {
            property = new Property()
            property.set("domainClassName", image.getStr("class"))
            property.set("domainIdent", image.getLong("id"))
            property.set("key", it.key.toString())
            property.set("value", it.value.toString())
            props << property
        }
        def result = cytomine.addMultipleDomainProperties(props)
        log.info(result)

        if (projects && annotations.size() > 0) {
            log.info "${image.getStr("originalFilename")} annotations==="
            log.info annotations
            try {
                projects.each { idProject ->
                    def project = cytomine.getProject(idProject)
                    def ontology = cytomine.getOntology(project?.ontology)
                    def terms = ontology?.children?.collectEntries {[(it.name): it.id]}
                    def imageInstances = cytomine.getImageInstances(idProject).getList()

                    def imageInstance = null
                    while (imageInstance == null) {
                        imageInstance = imageInstances.find{it.baseImage == image.getLong("id")}
                        if (imageInstance) break
                        Thread.sleep(1000)
                    }

                    def annots = []
                    def annot
                    annotations.each { annotation ->
                        def idTerm = terms.find{it.key == annotation.term}?.value
                        if (!idTerm) {
                            //TODO: Simple user should be able to add a new term !
                            String pubKey = grailsApplication.config.cytomine.imageServerPublicKey
                            String privKey = grailsApplication.config.cytomine.imageServerPrivateKey
                            Cytomine cytomine2 = new Cytomine(cytomine.getHost(), pubKey, privKey)
                            def term = cytomine2.addTerm(annotation.term, "#AAAAAA", ontology?.id)
                            terms << [(term?.name): term?.id]
                            idTerm = term?.id
                        }

                        def propertiesList = []
                        annotation.properties.collectEntries {
                            propertiesList << [key: it.key, value: it.value]
                        }

                        annot = new Annotation()
                        annot.set("location", annotation.location as String)
                        annot.set("image", imageInstance?.id as Long)
                        annot.set("project", idProject)
                        annot.set("term", idTerm ? [idTerm as Long] : null)
                        annot.set("properties", propertiesList)
                        annots << annot
                    }

                    def success = false, count  = 0
                    while (!success && count < 100) {
                        try  {
                            result = cytomine.addMultipleAnnotations(annots)
                            log.info "${result} for image instance ${imageInstance?.id}"
                            success = true
                        }
                        catch(Exception e) {
                            log.error("ERROR DURING ANNOTATION ADD: " + e.printStackTrace())
                            Thread.sleep(1800)
                        }
                        count++
                    }
                }
            } catch(Exception e) {
                log.error("ERROR DURING ANNOTATION ADD (2): " + e.printStackTrace())
            }

            Thread.sleep(1800)
        }

        return image
    }

    private void groupImages(Cytomine cytomine, def newFiles, Long idProject) {
        if (newFiles.size() == 1) return
        //Create one imagegroup for this multidim image
        ImageGroup imageGroup = cytomine.addImageGroup(idProject)
        ImageInstanceCollection collection = cytomine.getImageInstances(idProject)

        newFiles.each { file ->
            def path = file.path
            def idImage = 0L
            while (idImage == 0) {
                log.info "Wait for " + path

                for (int i = 0; i < collection.size(); i++) {
                    Long expected = file.id
                    Long current = collection.get(i).get("baseImage")
                    if (expected.equals(current)) {
                        log.info "OK!"
                        idImage = collection.get(i).getId()
                    }
                }

                if (idImage != 0) break

                Thread.sleep(1000)
                collection = cytomine.getImageInstances(idProject)
            }
            log.info "Uploaded ImageID: $idImage"
            //Add this image to current imagegroup
            ImageSequence imageSequence = cytomine.addImageSequence(imageGroup.getId(), idImage,
                    Integer.parseInt(file.z), 0, Integer.parseInt(file.t), Integer.parseInt(file.c))
            log.info "new ImageSequence: " + imageSequence.get("id")
        }
    }

    private def createResponseContent(def filename, def size, def contentType, def uploadedFileJSON, def images) {
        def content = [:]
        content.status = 200
        content.name = filename
        content.size = size
        content.type = contentType
        content.uploadFile = uploadedFileJSON
        content.images = images
        return content
    }
}
