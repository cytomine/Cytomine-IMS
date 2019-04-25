package be.cytomine.server

/*
 * Copyright (c) 2009-2019. Authors: see NOTICE file.
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

import be.cytomine.client.CytomineException
import be.cytomine.client.models.*
import be.cytomine.exception.DeploymentException
import be.cytomine.exception.FormatException
import be.cytomine.exception.MiddlewareException
import be.cytomine.formats.*
import be.cytomine.formats.archive.ArchiveFormat
import be.cytomine.formats.supported.NativeFormat
import utils.FilesUtils

import java.nio.file.Paths

class UploadService {

    final static public String UNDERTERMINED_CONTENT = "undetermined"

    def executorService //used for the runAsync
    def grailsApplication
    def cytomineService
    def fileSystemService

    /**
     * Upload a file on Cytomine.
     * It will create all intermediate files and final abstract images.
     *
     * @param user The Cytomine user doing the request
     * @param storage The Cytomine virtual storage in which the file is uploaded
     * @param filename The filename of the uploaded file
     * @param filePath The path where the file is currently stored
     * @param isSync Whether the HTTP response waits for the end of deployment
     * @param projects A list of Cytomine projects to which the final abstract images are linked
     * @param properties A map of custom properties to associate to the final abstract images
     *
     * @return A map with the root UploadedFile and all the generated AbstractImages
     */
    def upload(User user, Storage storage, String filename, def filePath, boolean isSync, def projects, def properties) {
        if (!filePath) {
            throw new FileNotFoundException("Got an invalid file. Disk can be full.")
        }

        def temporaryFile = new File(filePath)
        if (!temporaryFile.exists()) {
            throw new FileNotFoundException(temporaryFile.absolutePath + " NOT EXIST!")
        }

        def imsServer = cytomineService.getThisImageServer()
        if (!imsServer)
            throw new MiddlewareException("IMS reference not found in core.")

        log.info "ImageServer: $imsServer"

        log.info "Create an uploadedFile instance and copy it to its storage"
        log.info "filePath: $filePath"
        log.info "absoluteFilePath: ${temporaryFile.absolutePath}"

        // Root UF
        def size = temporaryFile.size()
        def extension = FilesUtils.getExtensionFromFilename(filename).toLowerCase()
        def status = UploadedFile.Status.UPLOADED
        def destinationPath = Paths.get(new Date().getTime().toString(), FilesUtils.correctFilename(filename))
        def uploadedFile = new UploadedFile(imsServer, filename, destinationPath.toString(), size, extension,
                UNDERTERMINED_CONTENT, projects, storage, user, status, null).save()

        File file = new File((String) uploadedFile.get('path'))
        fileSystemService.move(temporaryFile, file)

        def result = [:]
        result.uploadedFile = uploadedFile

        def uploadInfo = [
                user      : user,
                storage   : storage,
                imsServer : imsServer,
                isSync    : isSync,
                projects  : projects,
                properties: properties
        ]

        if (isSync) {
            log.info "Sync upload"
            deployFile(file, uploadedFile, uploadInfo, result)
        } else {
//            runAsync {
                log.info "Async upload"
                deployFile(file, uploadedFile, uploadInfo, result)
//            }
        }

        log.info result
        return result
    }

    /**
     * Deploy the received file. It results in a set of abstract images on which custom properties are added and image instances are created for the given projects.
     *
     * At the end, the uploaded file has the status
     * - DEPLOYED if everything happened correctly
     * - ERROR_DEPLOYMENT otherwise
     *
     * @param currentFile The physical representation of uploaded file
     * @param uploadedFile The Cytomine representation of uploaded file
     * @param uploadInfo A map with information about the upload
     * @param result A map filled with deployment results (method output)
     * @return
     */
    private def deployFile(File currentFile, UploadedFile uploadedFile, def uploadInfo, def result) {
        try {
            def file = new CytomineFile(currentFile.absolutePath)
            def deployed = deploy(file, uploadedFile, null, null, uploadInfo)

            // Add properties to resulting abstract images
            deployed.images.each { image ->
                def props = []
                uploadInfo.properties.each {
                    props << new Property(image, (String) it.key, (String) it.value)
                }
                if (props.size() > 0) {
                    log.info "Add custom ${props.size()} properties to ${image}"
                    log.debug properties
                    props.each { it.save() } // TODO: replace by a multiple add in a single request
                }
            }

            // Add images to projects
            def imageInstances = []
            for (int i = 0; i < uploadInfo.projects.size(); i++) {
                def project = uploadInfo.projects.get(i)
                deployed.images.each { image ->
                    log.info "Adding ${image} to $project"
                    imageInstances << new ImageInstance(image, project).save()
                }
            }

            result.images = deployed.images
            result.slices = deployed.slices
            result.instances = imageInstances

            // Set uploaded file as deployed
            uploadedFile = new UploadedFile().fetch(uploadedFile.id)
            uploadedFile.changeStatus(UploadedFile.Status.DEPLOYED)

        } catch (DeploymentException | CytomineException e) {
            uploadedFile = new UploadedFile().fetch(uploadedFile.id)

            // Errors have an odd code.
            if (uploadedFile.get("status") % 2 != 0) {
                uploadedFile.changeStatus(UploadedFile.Status.ERROR_DEPLOYMENT)
            }

            if (uploadInfo.isSync) {
                throw new DeploymentException(e.getMessage())
            } else {
                e.printStackTrace()
            }
        }
    }


    /**
     * Recursively detect, convert and deploy the current file into Cytomine until a conversion leads to a format natively supported by Cytomine.
     *
     * @param currentFile The "physical" representation of the current file to deploy
     * @param uploadedFile The Cytomine representation of current file. If null, it is generated.
     * @param uploadedFileParent The Cytomine representation of the current file's parent
     * @param abstractImage The Cytomine representation of the image. Null until the file is an image.
     * @param uploadInfo A map with information about the upload
     *
     * @return A list of map [image: AbstractImage, slices: [AbstractSlice]]
     */
    private def deploy(CytomineFile currentFile, UploadedFile uploadedFile, UploadedFile uploadedFileParent, AbstractImage abstractImage, def uploadInfo) {

        log.info "Deploy $currentFile"
        def result = [images: [], slices: []]

        def identifier = new FormatIdentifier(new CytomineFile(currentFile.absolutePath))

        // If the current file is a regular folder, recursively deploy its children but do not create an UF
        if (identifier.isClassicFolder()) {
            def errors = []
            currentFile.listFiles().each {
                if (it.name == ".DS_STORE" || it.name.startsWith("__MACOSX"))
                    return

                try {
                    def child = new CytomineFile(it.absolutePath)
                    def deployed = deploy(child, null, uploadedFile ?: uploadedFileParent, abstractImage, uploadInfo)
                    result.images.addAll(deployed.images)
                    result.slices.addAll(deployed.slices)
                } catch (DeploymentException e) {
                    errors << e.getMessage()
                }
            } // TODO: parallelize

            if (!errors.isEmpty()) {
                throw new DeploymentException(errors.join("\n"))
            }

            return result
        }

        // Create a Cytomine representation for the current file if not yet done
        if (uploadedFile == null) {
            String filename = currentFile.name
            String destPath = currentFile.path - (uploadedFileParent.getStr("path") - uploadedFileParent.getStr("filename"))
            Long size = (currentFile.isDirectory()) ? currentFile.directorySize() : currentFile.size()
            String extension = (String) currentFile.extension()

            log.info "Create a new uploadedFile for $filename"
            uploadedFile = new UploadedFile(uploadInfo.imsServer, filename, destPath, size, extension,
                    UNDERTERMINED_CONTENT, uploadInfo.projects, uploadInfo.storage, uploadInfo.user,
                    UploadedFile.Status.DETECTING_FORMAT, uploadedFileParent).save()

            log.info "New uploaded file: ${uploadedFile.toString()}"
        }

        Format format
        try {
            format = identifier.identify()
        } catch (FormatException e) {
            log.warn "Undetected format" + e.toString()
            uploadedFile.changeStatus(UploadedFile.Status.ERROR_FORMAT)
            throw new DeploymentException(e.getMessage())
        }

        log.info "Detected format = $format"
        uploadedFile.set("contentType", format.mimeType)
        uploadedFile.update()

        if (!abstractImage && !(format instanceof ArchiveFormat)) {
            try {
                def metadata = format.cytomineProperties()
                abstractImage = createAbstractImage(uploadedFile, metadata)
                result.images.add(abstractImage)
            }
            catch (CytomineException e) {
                uploadedFile.changeStatus(UploadedFile.Status.ERROR_DEPLOYMENT)
                throw new DeploymentException(e.getMessage())
            }

        }

        if (format instanceof NativeFormat) {
            uploadedFile.set("status", UploadedFile.Status.DEPLOYING.code)
            log.info uploadedFile.get("status")

            if (format instanceof MultipleFilesFormat) {
                File root = format.getRootFile(currentFile)
                uploadedFile.set("originalFilename", root.name)
                uploadedFile.set("filename", root.absolutePath - (uploadedFile.getStr("path") - uploadedFile.getStr("filename")))
            }

            if (format instanceof CustomExtensionFormat) {
                File renamed = format.rename()
                uploadedFile.set("filename", renamed.absolutePath - (uploadedFile.getStr("path") - uploadedFile.getStr("filename")))
            }

            uploadedFile.update()

            try {
                AbstractSlice slice = createAbstractSlice(uploadedFile, abstractImage, format, currentFile)
                result.slices.add(slice)
                // fetch to get the last uploadedFile with the image
                uploadedFile = new UploadedFile().fetch(uploadedFile.id)
                if (abstractImage.getLong("uploadedFile") != uploadedFile.getId()) {
                    uploadedFile.changeStatus(UploadedFile.Status.DEPLOYED)
                }
            } catch (CytomineException e) {
                uploadedFile.changeStatus(UploadedFile.Status.ERROR_DEPLOYMENT)
                throw new DeploymentException(e.getMsg())
            }
        } else {
            uploadedFile.changeStatus(UploadedFile.Status.CONVERTING)

            def errors = []
            def files = []

            try {
                files = format.convert()
            }
            catch (/*ConversionException |*/ Exception e) {
                errors << e.getMessage()
            }

            files.each {
                try {
                    def deployed = deploy(it as CytomineFile, null, uploadedFile, abstractImage, uploadInfo)
                    result.images.addAll(deployed.images)
                    result.slices.addAll(deployed.slices)
                }
                catch (DeploymentException e) {
                    errors << e.getMessage()
                }
            } //TODO: parallelize + pipeline

            if (!errors.isEmpty()) {
                uploadedFile.changeStatus(UploadedFile.Status.ERROR_CONVERSION)
                throw new DeploymentException(errors.join("\n"))
            } else {
                uploadedFile.changeStatus(UploadedFile.Status.CONVERTED)
            }
        }

        return result
    }

    private AbstractImage createAbstractImage(UploadedFile uploadedFile, def metadata) {
        def image = new AbstractImage(uploadedFile, uploadedFile.getStr('originalFilename')).save()

        def props = []
        metadata.each {
            props << new Property(image, (String) it.key, (String) it.value)
        }

        if (props.size() > 0) {
            log.info "Add ${props.size()} format-dependent properties to ${image}"
            log.debug properties
            props.each { it.save() } // TODO: replace by a multiple add in a single request
        }
        image.extractUsefulProperties()

        return image
    }

    private AbstractSlice createAbstractSlice(UploadedFile uploadedFile, AbstractImage image, Format format, CytomineFile file) {
        def slice = new AbstractSlice(image, uploadedFile, format.mimeType, file.c as Double, file.z as Double, file.t as Double).save()
        return slice
    }
}
