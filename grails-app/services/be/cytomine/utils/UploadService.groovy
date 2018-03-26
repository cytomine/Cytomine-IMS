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
import be.cytomine.client.CytomineException
import be.cytomine.client.collections.ImageInstanceCollection
import be.cytomine.client.models.ImageGroup
import be.cytomine.client.models.ImageSequence
import be.cytomine.client.models.Storage
import be.cytomine.client.models.UploadedFile
import be.cytomine.exception.DeploymentException
import be.cytomine.exception.FormatException
import be.cytomine.formats.ArchiveFormat
import be.cytomine.formats.Format
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.IConvertableImageFormat
import be.cytomine.formats.heavyconvertable.BioFormatConvertable
import be.cytomine.formats.heavyconvertable.IHeavyConvertableImageFormat
import be.cytomine.formats.lightconvertable.VIPSConvertable
import grails.util.Holders
import utils.FilesUtils

class UploadService {

    def executorService
    def deployImagesService

    // WARNING ! This function is recursive. Be careful !
    def upload(Cytomine cytomine, String filename, Long idStorage, String contentType, def filePath, def projects, long currentUserId, def properties, long timestamp, boolean isSync){

        def tmpUploadedFilePath = new File(filePath)
        log.info "filePath=$filePath"

        log.info "absoluteFilePath=${tmpUploadedFilePath.absolutePath}"
        if (!tmpUploadedFilePath.exists()) {
            throw new FileNotFoundException(tmpUploadedFilePath.absolutePath + " NOT EXIST!")
        }
        def size = tmpUploadedFilePath.size()
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
                "undetermined",//contentType,
                projects,
                [idStorage],
                currentUserId,
                0, // UPLOADED status
                null // idParent
        )



        deployImagesService.copyUploadedFile(cytomine, tmpUploadedFilePath.absolutePath, uploadedFile, [storage])

        File currentFile = new File(storage.getStr("basePath") + File.separator + uploadedFile.getStr("filename"))
        String currentPath = currentFile.parent

        try {
            deploy(cytomine, currentFile, /*currentPath, */uploadedFile, null)
        } catch(DeploymentException | CytomineException e) {
            int status = uploadedFile.get("status")
            if (status != 3 && status != 8 && status != 9){
                println "HEERRRREEE "+status
                cytomine.editUploadedFile(uploadedFile.id, 9) // status ERROR_DEPLOYMENT
            }
        }

        /*
        String storageBufferPath = Holders.config.cytomine.storageBufferPath
        log.info "Image are tmp convert in $storageBufferPath"
        String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join("")


        def imageFormats;
        try{
            imageFormats = FormatIdentifier.getImageFormats(originalFilenameFullPath)
        } catch(FormatException e){
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

            def tmpResponseContent = [];
            files.each { file ->
                def path = file.path;
                def nameNewFile = path.substring(path.lastIndexOf("/")+1)
                // maybe redetermine the contentType ?
                // recursion
                def tmp = upload(cytomine, nameNewFile,idStorage, contentType, path, projects, currentUserId, properties, timestamp, true)[0]
                tmp.z = file.z
                tmp.t = file.t
                tmp.c = file.c
                tmpResponseContent << tmp

            }

            if(image.imageFormat instanceof BioFormatConvertable && image.imageFormat.group) {

                def newFiles = [];
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
                    groupImages(cytomine, newFiles, project);
                }
            }

            int status = tmpResponseContent.size() > 0 && tmpResponseContent?.status.unique() == [200] ? 1 : 4;
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
            runAsync {
                convertAndCreate();
                heavyConvertableImageFormats.each {
                    println "unsupported image "+it
                    conversion(it);
                };
                log.info "image async = $images"
            }
        }

        def responseContent = [createResponseContent(filename, size, contentType, uploadedFile.toJSON(),images)]

        return responseContent;
        */
    }

    private def deploy(Cytomine cytomine, File currentFile, /*String currentPath, */UploadedFile uploadedFile, UploadedFile uploadedFileParent){

        println "File currentFile, UploadedFile uploadedFile, UploadedFile uploadedFileParent"
        println currentFile
        println uploadedFile
        println uploadedFileParent


        if(FormatIdentifier.isClassicFolder(currentFile.path)){
            boolean errorFlag = false
            // passe aux childs
            currentFile.listFiles().each {
                try{
                    deploy(cytomine, it, null, uploadedFile)
                } catch(DeploymentException e){
                    errorFlag = true
                }
            }
            if(errorFlag){
                throw new DeploymentException()
            }
            return
        }

        if(uploadedFile == null){
            // create UF
            String filename = currentFile.name

            println "filename"
            println filename
            println currentFile.path
            println currentFile.path.replace(uploadedFileParent.getStr("path"),"")

            String destPath = currentFile.path.replace(uploadedFileParent.getStr("path"),"")

            uploadedFile = cytomine.addUploadedFile(
                    filename,
                    destPath,
                    uploadedFileParent.getStr("path"),
                    currentFile.size(),
                    (String) FilesUtils.getExtensionFromFilename(currentFile.path).toLowerCase(),
                    "undetermined", //contentType,
                    uploadedFileParent.getList("projects"),
                    uploadedFileParent.getList("storages"),
                    uploadedFileParent.getLong("user"),
                    6L, // TODEPLOY status
                    uploadedFileParent.id // idParent
            )

        }

        Format format
        try{
            format = FormatIdentifier.testGetImageFormat(currentFile.path)
        } catch(FormatException e){
            log.warn "Undetected format"
            log.warn e.toString()
            cytomine.editUploadedFile(uploadedFile.id, 3) // status ERROR FORMAT
            throw new DeploymentException(e)
        }

        println "format"
        println format
        println format.mimeType

        uploadedFile.set("contentType", format.mimeType);
        uploadedFile = (UploadedFile) cytomine.updateModel(uploadedFile)

        log.info "imageFormats = $format"


        if(format instanceof IConvertableImageFormat){
            cytomine.editUploadedFile(uploadedFile.id, 7) // status TO_CONVERT
            // MEttre les status comme var publique statique dans le nouveau client java
            boolean errorFlag = false
            def files = []
            try {
                println "convert"
                files = format.convert() // try catch et status conversion ERROR
                println files
            } catch (Exception e) {
                errorFlag = true
            }
            files.each {
                /* get les élément du uploadedFile parent pour feeder des trucs ici
                def uploadedFileChild = cytomine.addUploadedFile(
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
                )*/
                try {
                    deploy(cytomine, new File(it), null, uploadedFile)
                } catch(DeploymentException e){
                    errorFlag = true
                    //  ???
                }

                if(format instanceof BioFormatConvertable && format.group) {

                    // grouper les images. J'ai besoin de l'uploaded file qui a été créé
                    def newFiles = [];
                    /*tmpResponseContent.each { tmp ->
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
                        groupImages(cytomine, newFiles, project);
                    }*/
                }


            }
            if(errorFlag){
                cytomine.editUploadedFile(uploadedFile.id, 8) // status ERROR CONVERSION
                throw new DeploymentException()
            } else {
                cytomine.editUploadedFile(uploadedFile.id, 1) // status CONVERTED
            }
        } else {
            //creation AI
            try {
                createAbstractImage(cytomine, format, uploadedFile)
                cytomine.editUploadedFile(uploadedFile.id, 2) // status DEPLOYED
            } catch(CytomineException e) {
                cytomine.editUploadedFile(uploadedFile.id, 9) // status ERROR_DEPLOYMENT
                throw new DeploymentException(e)
            }
        }

        return uploadedFile

    }

    private def createAbstractImage(Cytomine cytomine, Format format, UploadedFile uploadedFile) {
        cytomine.addNewImage(uploadedFile.id, uploadedFile.get('filename'), uploadedFile.get('filename'), format.mimeType)
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
        ImageInstanceCollection collection = cytomine.getImageInstances(idProject);

        newFiles.each { file ->
            def path = file.path;
            def idImage = 0L
            while (idImage == 0) {
                log.info "Wait for " + path;

                for (int i = 0; i < collection.size(); i++) {
                    Long expected = file.id;
                    Long current = collection.get(i).get("baseImage");
                    if (expected.equals(current)) {
                        log.info "OK!";
                        idImage = collection.get(i).getId();
                    }
                }

                if(idImage != 0) break;

                Thread.sleep(1000);
                collection = cytomine.getImageInstances(idProject);
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
