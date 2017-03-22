package be.cytomine.utils

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
import be.cytomine.client.models.ImageGroup
import be.cytomine.client.models.ImageSequence
import be.cytomine.client.models.Storage
import be.cytomine.client.models.UploadedFile
import be.cytomine.formats.ArchiveFormat
import be.cytomine.formats.Format
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.heavyconvertable.BioFormatConvertable
import be.cytomine.formats.heavyconvertable.IHeavyConvertableImageFormat
import be.cytomine.formats.lightconvertable.VIPSConvertable
import grails.util.Holders
import utils.FilesUtils

class UploadService {

    def backgroundService
    def deployImagesService

    // WARNING ! This function is recursive. Be careful !
    def upload(Cytomine cytomine, String filename, Long idStorage, String contentType, def filePath, def projects, long currentUserId, def properties, long timestamp, boolean isSync){

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
                null // idParent
        )

        deployImagesService.copyUploadedFile(cytomine, uploadedFilePath.absolutePath, uploadedFile, [storage])


        String storageBufferPath = Holders.config.cytomine.storageBufferPath
        log.info "Image are tmp convert in $storageBufferPath"
        String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join("")


        def imageFormats = FormatIdentifier.getImageFormats(
                originalFilenameFullPath
        )

        log.info "imageFormats = $imageFormats"

        if (imageFormats.size() == 0) { //not a file that we can recognize
            //todo: response error message
            uploadedFile = cytomine.editUploadedFile(uploadedFile.id, 3) // status ERROR FORMAT
            return
        }


        def heavyConvertableImageFormats = imageFormats.findAll {it.imageFormat==null || it.imageFormat instanceof IHeavyConvertableImageFormat}; // remove the check ==null
        imageFormats = imageFormats - heavyConvertableImageFormats;

        def images = []

        def convertAndCreate = {
            cytomine.editUploadedFile(uploadedFile.id, 6) //to deploy
            imageFormats.each {
                images << createImage(cytomine,it,filename,storage,contentType,projects,idStorage,currentUserId,properties, uploadedFile)
            }
            cytomine.editUploadedFile(uploadedFile.id, 2) //deployed
        };

        def conversion = { image ->

            def files = image.imageFormat.convert();

            def tmpResponseContent;
            files.each { file ->
                def path = file.path;
                def nameNewFile = path.substring(path.lastIndexOf("/")+1)
                // maybe redetermine the contentType ?
                // recursion
                tmpResponseContent = upload(cytomine, nameNewFile,idStorage, contentType, path, projects, currentUserId, properties, timestamp, true)

            }

            if(image.imageFormat instanceof BioFormatConvertable && image.imageFormat.group) {

                def newFiles = [];
                files.each { file ->
                    def newFile = [:]
                    def outputImage = tmpResponseContent[0].images[0]
                    newFile.path = outputImage.get("filename")
                    newFile.id = outputImage.id
                    newFile.z = file.z
                    newFile.t = file.t
                    newFile.c = file.c
                    newFiles << newFile
                }

                projects.each { project ->
                    groupImages(cytomine, newFiles, project);
                }
            }

            int status = tmpResponseContent.size() > 0 && tmpResponseContent[0]?.status == 200 ? 1 : 4;
            cytomine.editUploadedFile(uploadedFile.id, status) // converted or error conversion
        };

        if(isSync) {
            log.info "Execute convert & deploy NOT in background (sync=true!)"
            convertAndCreate();
            heavyConvertableImageFormats.each {
                println "unsupported image "+it
                conversion(it);
            };
            log.info "image sync = $images"
        } else {
            log.info "Execute convert & deploy into background"
            backgroundService.execute("convertAndDeployImage", {
                convertAndCreate();
                heavyConvertableImageFormats.each {
                    println "unsupported image "+it
                    conversion(it);
                };
                log.info "image async = $images"
            })
        }

        def responseContent = [createResponseContent(filename, size, contentType, uploadedFile.toJSON(),images)]

        return responseContent;
    }

    private def createImage(Cytomine cytomine, def imageFormatsToDeploy, String filename, Storage storage,def contentType, List projects, long idStorage, long currentUserId, def properties, UploadedFile uploadedFile) {
        log.info "createImage $imageFormatsToDeploy"

        Format imageFormat = imageFormatsToDeploy.imageFormat
        String originalName = new File(imageFormat.absoluteFilePath).name


        Format parentImageFormat = imageFormatsToDeploy.parent?.imageFormat

        if(imageFormat instanceof VIPSConvertable) {
            String newImage = imageFormat.convert();
            imageFormat = FormatIdentifier.getImageFormat(newImage)
            imageFormatsToDeploy = [uploadedFilePath:newImage, imageFormat:imageFormat]
        }

        // TODO find a way to unify absoluteFilePath & uploadedFilepath
        String path = imageFormatsToDeploy.uploadedFilePath ?: imageFormatsToDeploy.absoluteFilePath
        String shortPath = path.replace(storage.getStr("basePath"), "")

        def childUploadedFile = null
        if (parentImageFormat instanceof ArchiveFormat) {
            File f = new File(imageFormat.absoluteFilePath)
            contentType = imageFormatsToDeploy.imageFormat.mimeType ?: URLConnection.guessContentTypeFromName(f.getName());

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

        def image = cytomine.addNewImage(finalParent.id, shortPath, finalParent.get('filename'), imageFormatsToDeploy.imageFormat.mimeType)

        log.info "properties"
        log.info properties
        properties.each {
            log.info "it.key"
            log.info it.key
            log.info "it.value"
            log.info it.value
            cytomine.addDomainProperties(image.getStr("class"),image.getLong("id"),it.key.toString(),it.value.toString())
        }
        return image
    }

    private void groupImages(Cytomine cytomine, def newFiles, Long idProject) {
        if(newFiles.size() == 1) return;
        //Create one imagegroup for this multidim image
        ImageGroup imageGroup = cytomine.addImageGroup(idProject)

        newFiles.each { file ->
            def path = file.path;
            def idImage = 0L
            while (idImage == 0) {
                log.info "Wait for " + path;

                ImageInstanceCollection collection = cytomine.getImageInstances(idProject);

                for (int i = 0; i < collection.size(); i++) {
                    Long expected = file.id;
                    Long current = collection.get(i).get("baseImage");
                    if (expected.equals(current)) {
                        log.info "OK!";
                        idImage = collection.get(i).getId();
                    }
                }
                Thread.sleep(1000);
            }
            log.info "Uploaded ImageID: $idImage"
            //Add this image to current imagegroup
            ImageSequence imageSequence = cytomine.addImageSequence(imageGroup.getId(), idImage, Integer.parseInt(file.z), 0, Integer.parseInt(file.t), Integer.parseInt(file.c));
            log.info "new ImageSequence: " + imageSequence.get("id");
        }
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
