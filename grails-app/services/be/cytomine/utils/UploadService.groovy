package be.cytomine.utils

/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
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
import be.cytomine.client.models.AbstractImage
import be.cytomine.client.models.ImageGroup
import be.cytomine.client.models.ImageSequence
import be.cytomine.client.models.Storage
import be.cytomine.client.models.UploadedFile
import be.cytomine.exception.DeploymentException
import be.cytomine.exception.FormatException
import be.cytomine.formats.Format
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.IConvertableImageFormat
import be.cytomine.formats.heavyconvertable.BioFormatConvertable
import be.cytomine.formats.supported.PyramidalTIFFFormat
import be.cytomine.formats.supported.digitalpathology.OpenSlideMultipleFileFormat
import grails.converters.JSON
import utils.FilesUtils

class UploadService {

    def executorService //used for the runAsync
    def deployImagesService

    /**
     * Upload a file on Cytomine.
     * Will create all intermediate files and final abstract images.
     *
     * @param cytomine : Instance of the Cytomine client
     * @param filename : filename of the uploadedFile
     * @param idStorage : storage where the images will be stored
     * @param filePath : path where the image is currently stored
     * @param projects : AbstractImages & ImageGroups generated will be associated to this project
     * @param currentUserId
     * @param properties : list of key-value pair that will be associated to all the generated AbstractImages
     * @param timestamp :
     * @param isSync : boolean. true if we wait the end of deployment before returning an HTTP response
     * @return A map with the original uploadedFile and all the AbstractImages generated
     */
    def upload(Cytomine cytomine, String filename, Long idStorage, def filePath, def projects, long currentUserId, def properties, long timestamp, boolean isSync){

        if(!filePath) throw new FileNotFoundException("Got an invalid file. Disk can be full.")
        def tmpUploadedFilePath = new File(filePath)

        log.info "filePath=$filePath"
        log.info "absoluteFilePath=${tmpUploadedFilePath.absolutePath}"

        if (!tmpUploadedFilePath.exists()) {
            throw new FileNotFoundException(tmpUploadedFilePath.absolutePath + " NOT EXIST!")
        }
        def size = tmpUploadedFilePath.size()

        log.info "size=$size"
        log.info "Create an uploadedFile instance and copy it to its storages"

        String destPath = File.separator + timestamp.toString() + File.separator + FilesUtils.correctFileName(filename)
        Storage storage = new Storage().fetch(idStorage)

        log.info "storage.getStr(basePath) : " + storage.getStr("basePath")

        // no parent
        UploadedFile uploadedFile = new UploadedFile(
                filename,
                destPath,
                storage.getStr("basePath"),
                size,
                FilesUtils.getExtensionFromFilename(filename).toLowerCase(),
                "undetermined",//contentType,
                projects,
                [idStorage],
                currentUserId,
                UploadedFile.Status.UPLOADED,
                null // idParent
        ).save();


        deployImagesService.copyUploadedFile(cytomine, tmpUploadedFilePath.absolutePath, uploadedFile, [storage])

        File currentFile = new File(storage.getStr("basePath") + File.separator + uploadedFile.getStr("filename"))

        def result = [:]
        result.uploadedFile = uploadedFile

        if(isSync) {
            log.info "Sync upload"
            deployImagesAndGroups(cytomine, currentFile, uploadedFile, projects, properties, isSync, result)
        } else {
            runAsync {
                log.info "Async upload"
                deployImagesAndGroups(cytomine, currentFile, uploadedFile, projects, properties, isSync, result)
            }
        }

        return result
    }

    /**
     * Utility function for the runAsync. Will call the deploy function then group abstractimages into a project and add the properties
     * @param cytomine
     * @param currentFile
     * @param uploadedFile
     * @param projects
     * @param isSync
     * @param result : output of the function
     * @return The values are returned in the result object
     */
    private def deployImagesAndGroups(Cytomine cytomine, File currentFile, UploadedFile uploadedFile, def projects, def properties, boolean isSync, def result) {
        try {
            def deployed = deploy(cytomine, currentFile, uploadedFile, null)
            result.images = deployed.images

            projects.each { project ->
                groupImages(cytomine, deployed.groups, project);
            }

            log.info "properties"
            log.info properties
            result.images.each { image ->
                properties.each {
                    log.info "it.key"
                    log.info it.key
                    log.info "it.value"
                    log.info it.value
                    cytomine.addDomainProperties(image.getStr("class"),image.getLong("id"),it.key.toString(),it.value.toString())
                }
            }

        } catch(DeploymentException | CytomineException e) {
            int status = uploadedFile.get("status")
            if (status != 3 && status != 8 && status != 9){
                uploadedFile.changeStatus(UploadedFile.Status.ERROR_DEPLOYMENT)
            }
            if(isSync) {
                throw new DeploymentException(e.getMessage())
            }
        }
    }


    /**
     * Will deploy the currentFile into Cytomine with all intermediate conversions (recursion)
     * @param cytomine : Cytomine client
     * @param currentFile
     * @param uploadedFile : mandatory. if not present will be generated
     * @param uploadedFileParent : il uploadedFile os not present, we will create one with this parameter as parent
     * @return the map [images: array of AbstractImage objects, groups: array of groups of image]
     */
    // WARNING ! This function is recursive. Be careful !
    private def deploy(Cytomine cytomine, File currentFile, UploadedFile uploadedFile, UploadedFile uploadedFileParent){

        def result = [:]
        result.images = []
        result.groups = []

        if(FormatIdentifier.isClassicFolder(currentFile.path)){
            boolean errorFlag = false
            String errorMsg = "";
            currentFile.listFiles().each {
                try{
                    //a simple folder will not create an UploadedFile object
                    def deployed = deploy(cytomine, it, null, uploadedFile ?: uploadedFileParent)
                    result.images.addAll(deployed.images)
                    result.groups.addAll(deployed.groups)
                } catch(DeploymentException e){
                    errorFlag = true
                    errorMsg += e.getMessage()+"\n"
                }
            }
            if(errorFlag){
                throw new DeploymentException(errorMsg)
            }
            return result
        }

        if(uploadedFile == null){
            // create UF
            String filename = currentFile.name

            String destPath = currentFile.path.replace(uploadedFileParent.getStr("path"),"")

            Long size = currentFile.size()
            if(currentFile.isDirectory()){
                size = currentFile.directorySize()
            }

            uploadedFile = new UploadedFile(
                    filename,
                    destPath,
                    uploadedFileParent.getStr("path"),
                    size,
                    (String) FilesUtils.getExtensionFromFilename(currentFile.path).toLowerCase(),
                    "undetermined", //contentType,
                    uploadedFileParent.getList("projects"),
                    uploadedFileParent.getList("storages"),
                    uploadedFileParent.getLong("user"),
                    UploadedFile.Status.TO_DEPLOY,
                    uploadedFileParent.id // idParent
            ).save()
        }

        Format format
        try{
            format = FormatIdentifier.getImageFormat(currentFile.path)
        } catch(FormatException e){
            log.warn "Undetected format"
            log.warn e.toString()
            uploadedFile.changeStatus(UploadedFile.Status.ERROR_FORMAT)
            throw new DeploymentException(e)
        }

        log.info "Format = $format"

        uploadedFile.set("contentType", format.mimeType);
        uploadedFile = (UploadedFile) uploadedFile.update()

        if(format instanceof IConvertableImageFormat){
            uploadedFile.changeStatus(UploadedFile.Status.TO_CONVERT)
            boolean errorFlag = false
            String errorMsg = "";
            def files = []
            try {
                files = format.convert() // try catch et status conversion ERROR
            } catch (Exception e) {
                errorFlag = true
                errorMsg += e.getMessage()
            }

            if(format instanceof BioFormatConvertable && format.group) {
                def newFiles = [];
                files.each { file ->
                    file = JSON.parse(file)

                    //sort par file path !!!

                    def imgs = [];
                    try {
                        imgs = deploy(cytomine, new File(file.path), null, uploadedFile).images
                        result.images.addAll(imgs)

                        assert imgs.size() == 1

                        def img = imgs[0]
                        def newFile = [:]
                        newFile.path = img.get("filename")
                        newFile.id = img.id
                        newFile.z = file.z
                        newFile.t = file.t
                        newFile.c = file.c
                        newFiles << newFile

                    } catch(DeploymentException e){
                        errorFlag = true
                        errorMsg += e.getMessage()
                    }
                }
                result.groups = newFiles
            } else {
                files.each { file ->
                    try {
                        def deployed = deploy(cytomine, new File(file), null, uploadedFile)
                        result.images.addAll(deployed.images)
                        result.groups.addAll(deployed.groups)
                    } catch (DeploymentException e) {
                        errorFlag = true
                        errorMsg += e.getMessage()
                    }
                }
            }

            if(errorFlag){
                uploadedFile.changeStatus(UploadedFile.Status.ERROR_CONVERSION)
                throw new DeploymentException(errorMsg)
            } else {
                uploadedFile.changeStatus(UploadedFile.Status.CONVERTED)
            }
        }
        else {

            if(format instanceof OpenSlideMultipleFileFormat) {
                File root = ((OpenSlideMultipleFileFormat) format).getRootFile(currentFile)

                uploadedFile.set("originalFilename", root.name)
                uploadedFile.set("filename", root.absolutePath.replace(uploadedFile.getStr("path"),""))
                uploadedFile = uploadedFile.update()
            }

            //creation AbstractImage
            try {
                AbstractImage image = createAbstractImage(cytomine, uploadedFile, uploadedFileParent,format)
                // fetch to get the last uploadedFile with the image
                uploadedFile.fetch(uploadedFile.id)
                uploadedFile.changeStatus(UploadedFile.Status.DEPLOYED)
                return [images : [image], groups: []]
            } catch(CytomineException e) {
                uploadedFile.changeStatus(UploadedFile.Status.ERROR_DEPLOYMENT)
                throw new DeploymentException(e.getMsg())
            }
        }

        return result
    }

    private AbstractImage createAbstractImage(Cytomine cytomine, UploadedFile uploadedFile, UploadedFile uploadedFileParent, def format) {
        String originalFilename

        if(format instanceof PyramidalTIFFFormat){
            if(uploadedFileParent.get("contentType").equals("application/zip")) {
                originalFilename = uploadedFile.get('originalFilename')
            }
            else
                originalFilename = uploadedFileParent.get('originalFilename')
        } else
            originalFilename = uploadedFile.get('originalFilename')


        AbstractImage image = cytomine.addNewImage(uploadedFile.id, uploadedFile.get('filename'), uploadedFile.get('filename'), originalFilename, format.mimeType)
        return image
    }

    private void groupImages(Cytomine cytomine, def newFiles, Long idProject) {
        if(newFiles.size() < 2) return;
        //Create one imagegroup for this multidim image
        ImageGroup imageGroup = new ImageGroup(idProject).save()
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
            ImageSequence imageSequence = new ImageSequence(imageGroup.getId(), idImage, Integer.parseInt(file.z), 0, Integer.parseInt(file.t), Integer.parseInt(file.c)).save()
            log.info "new ImageSequence: " + imageSequence.get("id");
        }
    }
}
