package be.cytomine.utils

/*
 * Copyright (c) 2009-2016. Authors: see NOTICE file.
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
import be.cytomine.exception.MiddlewareException
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.ImageFormat
import be.cytomine.formats.convertable.CellSensVSIFormat
import be.cytomine.formats.convertable.ConvertableFormat
import be.cytomine.formats.specialtiff.OMETIFFFormat
import grails.converters.JSON
import grails.util.Holders
import org.apache.commons.io.FilenameUtils
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


        String storageBufferPath = Holders.config.cytomine.storageBufferPath
        log.info "Image are tmp convert in $storageBufferPath"
        String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join("")


        def imageFormats = FormatIdentifier.getImageFormats(
                originalFilenameFullPath
        )

        log.info "imageFormats = $imageFormats"

        if (imageFormats.size() == 0) { //not a file that we can recognize
            //todo: response error message
            return
        }

        def unsupportedImageFormats = imageFormats.findAll {it.imageFormat==null || it.imageFormat instanceof ConvertableFormat || it.imageFormat instanceof CellSensVSIFormat || it.imageFormat instanceof OMETIFFFormat};
        imageFormats = imageFormats - unsupportedImageFormats;

        def images = []

        def convertAndCreate = {
            cytomine.editUploadedFile(uploadedFile.id, 6) //to deploy
            def imageFormatsToDeploy = convertImage(imageFormats, storageBufferPath)
            uploadedFile = cytomine.editUploadedFile(uploadedFile.id, 1)
            imageFormatsToDeploy.each {
                images << createImage(cytomine,it,filename,storage,contentType,projects,idStorage,currentUserId,properties, uploadedFile)
            }
        };

        def conversion = { image ->

            String inputPath = image.uploadedFilePath;
            if (inputPath == null) inputPath = image.absoluteFilePath;

            if(image.imageFormat == null || image.imageFormat instanceof CellSensVSIFormat || image.imageFormat instanceof OMETIFFFormat) {
                //if more than BioFormat change by if(image.imageFormat instanceof ConvertableToMultifile) where BioFormat is an extension

                // call Bioformat application and get an array of paths (the converted & splited images)
                boolean group = image.imageFormat instanceof OMETIFFFormat;
                def files = callConvertor(inputPath, group);

                def newFiles = [];

                files.each { file ->
                    def path = file.path;
                    def nameNewFile = path.substring(path.lastIndexOf("/")+1)
                    // maybe redetermine the contentType ?
                    // recursion
                    def tmpResponseContent = upload(cytomine, nameNewFile,idStorage, contentType, path, projects, currentUserId, properties, timestamp, true)

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
            } else {
                log.info "conversion DotSlide";
                String path = image.imageFormat.convert();
                def nameNewFile = path.substring(path.lastIndexOf("/")+1)
                // maybe redetermine the contentType ?
                // recursion
                upload(cytomine, nameNewFile,idStorage, contentType, path, projects, currentUserId, properties, timestamp, true)

            }
        };

        if(isSync) {
            log.info "Execute convert & deploy NOT in background (sync=true!)"
            convertAndCreate();
            if(unsupportedImageFormats.size()>0) {
                unsupportedImageFormats.each {
                    log.info "unsupported image "+it
                    /// can it be absoluteFilePath ?
                    conversion(it);
                };
            }
            cytomine.editUploadedFile(uploadedFile.id, 2) //deployed
            log.info "image sync = $images"
        } else {
            log.info "Execute convert & deploy into background"
            backgroundService.execute("convertAndDeployImage", {
                convertAndCreate();
                if(unsupportedImageFormats.size()>0) {
                    unsupportedImageFormats.each {
                        log.info "unsupported image "+it
                        /// can it be absoluteFilePath ?
                        conversion(it);
                    };
                }
                cytomine.editUploadedFile(uploadedFile.id, 2) //deployed
                log.info "image async = $images"
            })
        }

        def responseContent = [createResponseContent(filename, size, contentType, uploadedFile.toJSON(),images)]

        return responseContent;
    }

    private def convertImage(def filesToDeploy,String storageBufferPath) {
        //start to convert into pyramid format, if necessary
        def imageFormatsToDeploy = []
        filesToDeploy.each { fileToDeploy ->
            ImageFormat imageFormat = fileToDeploy.imageFormat
            String convertedImageFilename = imageFormat.convert(storageBufferPath)
            if (convertedImageFilename) {
                FormatIdentifier.getImageFormats(convertedImageFilename).each { convertedImageFormat ->
                    convertedImageFormat.parent = fileToDeploy
                    imageFormatsToDeploy << convertedImageFormat
                }
            //not necessary to convert it
            } else {
                fileToDeploy.parent = fileToDeploy
                imageFormatsToDeploy << fileToDeploy
            }
        }
        return imageFormatsToDeploy
    }

    private def createImage(Cytomine cytomine, def imageFormatsToDeploy, String filename, Storage storage,def contentType, List projects, long idStorage, long currentUserId, def properties, UploadedFile uploadedFile) {
        log.info "createImage $imageFormatsToDeploy"

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
                    uploadedFile.id, // this is the parent
                    null)//this is the Download parent

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
                finalParent.id, // this is the parent
                uploadedFile.id) //this is the Download parent

        log.info "_uploadedFile : "+_uploadedFile
        log.info "_uploadedFile.id : "+_uploadedFile.id
        def image = cytomine.addNewImage(_uploadedFile.id)

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

    private def callConvertor(String filePath, boolean group){

        if(!Boolean.parseBoolean(Holders.config.bioformat.application.enabled)) throw new MiddlewareException("Convertor BioFormat not enabled");

        log.info "BIOFORMAT called !"
        def files = [];
        String error;

        String hostName = Holders.config.bioformat.application.location
        int portNumber = Integer.parseInt(Holders.config.bioformat.application.port);

        try {
            Socket echoSocket = new Socket(hostName, portNumber);
            PrintWriter out =
                    new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader inp =
                    new BufferedReader(
                            new InputStreamReader(echoSocket.getInputStream()));

            out.println('{path:"'+filePath+'",group:'+group+'}');
            String result = inp.readLine();
            def json  = JSON.parse(result);
            files = json.files
            error = json.error;
        } catch (UnknownHostException e) {
            log.error(e.toString());
        }

        log.info "bioformat returns"
        log.info files

        if(files ==[] || files == null) {
            if (error != null) {
                throw new MiddlewareException("BioFormat Exception : \n"+error);
            }
        }
        return files
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
