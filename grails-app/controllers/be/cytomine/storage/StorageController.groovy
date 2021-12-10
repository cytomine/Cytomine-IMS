package be.cytomine.storage

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
import be.cytomine.client.CytomineConnection
import be.cytomine.client.CytomineException
import be.cytomine.client.HttpClient
import be.cytomine.client.collections.Collection
import be.cytomine.client.models.Project
import be.cytomine.client.models.Storage
import be.cytomine.client.models.User
import be.cytomine.exception.AuthenticationException
import be.cytomine.exception.DeploymentException
import grails.converters.JSON
import grails.util.Holders
import org.apache.http.HttpEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentProducer
import org.apache.http.entity.EntityTemplate
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.mime.content.StringBody
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import utils.FilesUtils

import java.nio.file.Path
import java.nio.file.Paths

@RestApi(name = "upload services", description = "Methods for uploading images")
class StorageController {

    def uploadService
    def cytomineService

    @RestApiMethod(description="Method for uploading an image")
    @RestApiParams(params=[
            @RestApiParam(name="files[]", type="data", paramType = RestApiParamType.QUERY, description = "The file content (Multipart)"),
            @RestApiParam(name="core", type = "String", paramType = RestApiParamType.QUERY, description = "URL of linked Cytomine-core"),
            @RestApiParam(name="storage", type="int", paramType = RestApiParamType.QUERY, description = "The id of the targeted storage"),
            @RestApiParam(name="projects", type="int", paramType = RestApiParamType.QUERY, description = " The ids of the targeted projects", required = false),
            @RestApiParam(name="sync", type="boolean", paramType = RestApiParamType.QUERY, description = "Whether operations are done synchronously or not (false by default)", required = false),
            @RestApiParam(name="keys", type="String", paramType = RestApiParamType.QUERY, description = "The keys of the properties to link with image, separated by comma", required = false),
            @RestApiParam(name="values", type="String", paramType = RestApiParamType.QUERY, description = "The values of the properties link with image, separated by comma", required = false)
    ])
    def upload () {
        try {
            // Backwards compatibility
            if (params.cytomine) params.core = params.cytomine
            if (params.idStorage) params.storage = params.idStorage
            if (params.idProject) params.projects = params.idProject

            String coreURL = params.core
            String ISPublicKey = grailsApplication.config.cytomine.ims.server.publicKey
            String ISPrivateKey = grailsApplication.config.cytomine.ims.server.privateKey
            log.info "Upload is made on Cytomine = $coreURL with image server $ISPublicKey/$ISPrivateKey key pair"
            CytomineConnection imsConnection = Cytomine.connection(coreURL, ISPublicKey, ISPrivateKey, true)

            // Check user authentication
            def authorization = cytomineService.getAuthorizationFromRequest(request)
            def messageToSign = cytomineService.getMessageToSignFromRequest(request)

            def keys = Cytomine.getInstance().getKeys(authorization.publicKey)
            log.info (keys.getAttr())
            if (!keys)
                throw new AuthenticationException("Auth failed: User not found! May be ImageServer user is not an admin!")

            if (!cytomineService.testSignature(keys.get('privateKey'), authorization.signature, messageToSign))
                throw new AuthenticationException("Auth failed.")

            CytomineConnection userConnection = new CytomineConnection(coreURL, (String) keys.get('publicKey'), (String) keys.get('privateKey'))
            def user = userConnection.getCurrentUser()

            // Check and get storage
            def storage = new Storage().fetch(userConnection, params.long('storage'))

            // Check and get projects
            def projects = new Collection(Project.class, 0, 0)
            params.list('projects').each {
                projects.add(new Project().fetch(userConnection, Long.parseLong(it)))
            }

            // Get properties
            def propertyKeys = params.list("keys")
            def values = params.list("values")
            if (propertyKeys.size() != values.size()) {
                throw new Exception("Key.size <> Value.size!")
            }
            def properties = [propertyKeys, values].transpose().collectEntries()

            // Get other parameters
            boolean isSync = params.boolean('sync')
            String filename = (String) params['files[].name']
            def filePath = (String) params['files[].path']

            log.info "Upload request"
            log.info "---> User: $user"
            log.info "---> Storage: $storage"
            log.info "---> Projects: $projects"
            log.info "---> Properties: $properties"
            log.info "---> Synchronous: $isSync"
            log.info "---> Filename: $filename"
            log.info "---> Filepath: $filePath"


            if (FilesUtils.getExtensionFromFilename(filename).toLowerCase().equals("isyntax")) {
                def temporaryFile = new File(filePath)
                Path source = Paths.get(filePath);
                String tmpFile = new Random().nextInt().toString() + new Date().getTime()
//                Path newdir = Paths.get(Holders.config.cytomine.ims.pims.bufferedPath,tmpDir);
//                if (!newdir.toFile().mkdirs()) {
//                    log.error("Cannot create tmp dir " + new File(tmpDir).getAbsolutePath())
//                    throw new IOException("Cannot create tmp dir " + new File(tmpDir).getAbsolutePath())
//                }

                Path newFile = Paths.get(Holders.config.cytomine.ims.pims.bufferedPath, tmpFile+"."+FilesUtils.getExtensionFromFilename(filename).toLowerCase());
                log.info "Rename file from ${source.toFile().toString()} to ${newFile.toFile().toString()}"
                if (!source.toFile().renameTo(newFile.toFile())) {
                    log.error("Cannot move file to pims pending path")
                }

                def responseContent = [:]
                responseContent.status = 200;
                uploadToPims(newFile.toFile().getAbsolutePath(), filename, temporaryFile.size(), userConnection, storage.getLong("id"))
                render responseContent as JSON
            } else {
                def responseContent = [:]
                try {
                    responseContent.status = 200;
                    responseContent.name = filename
                    def uploadResult = uploadService.upload(userConnection, storage as Storage, filename, filePath, isSync, projects, properties)

                    responseContent.uploadedFile = uploadResult.uploadedFile.getAttr()

                    def images = []
                    uploadResult.images.each { image ->
                        def slices = uploadResult.slices.find {it.getLong('image') == image.getId()}
                        def instances = uploadResult.instances.find { it.getLong('baseImage') == image.getId()}
                        images << [
                                image: image.getAttr(),
                                slices: slices.collect {it.getAttr()},
                                imageInstances: instances.collect {it.getAttr()}
                        ]
                    }
                    responseContent.images = images


                } catch(DeploymentException e){
                    response.status = 500;
                    responseContent.status = 500;
                    responseContent.error = e.getMessage()
                    responseContent.files = [[name:filename, size:0, error:responseContent.error]]
                }

                responseContent = [responseContent]

                render responseContent as JSON
            }



        }
        catch (CytomineException e) {
            log.error(e.toString())
            log.error(e.getMessage())
            log.error(e.printStackTrace())
            response.status = e.getHttpCode()
            render e.toString()
        }
        catch (Exception e) {
            log.error(e.toString())
            log.error(e.printStackTrace())
            response.status = 400
            render e.getCause().toString()
        }
    }

    private void uploadToPims(String path, String filename, Long size, CytomineConnection connection, Long storage) {
        if (!Holders.config.cytomine.ims.pims.enabled) {
            throw new Exception("PIMS is disabled")
        }

        String url = Holders.config.cytomine.ims.pims.url
        HttpClient client = null;

        MultipartEntity entity = new MultipartEntity();
        entity.addPart("files[].path", new StringBody(path));
        entity.addPart("files[].name", new StringBody(filename));
        entity.addPart("files[].size", new StringBody(size.toString()));

        client = new HttpClient(
                connection.getPublicKey(),
                connection.getPrivateKey(),
                url
        );
        String urlPath = "/upload?storage=$storage&core=${connection.getHost()}";
        client.authorize("POST", urlPath, entity.getContentType().getValue(), "application/json,*/*");
        client.connect(url + urlPath);
        int code = client.post(entity);
        log.debug("code=" + code);
        String response = client.getResponseData();
        log.debug("response=" + response);
        client.disconnect();
    }

    @RestApiMethod(description="Method for getting used and free space of the image storage")
    def size () {
        String storagePath = Holders.config.cytomine.ims.path.storage
        def proc = "df $storagePath".execute()
        proc.waitFor()

        String[] out = proc.text.split("\n")[1].trim().replaceAll("\\s+", " ").split(" ")

        Long used = Long.parseLong(out[2])
        Long available = Long.parseLong(out[3])

        String hostname = ""
        String mount = ""
        if (out[0].contains(":")) { // NFS mount
            hostname = out[0].split(":")[0]
            mount = out[0].split(":")[1]
        } else {
            hostname = "hostname".execute().text.split("\n")[0]
            mount = out[5]
        }

        String ip = "host $hostname".execute().text
        if (ip.contains("not found")) {
            ip = null
        } else {
            ip = ip.split(" ").last().hashCode()
        }

        def result = [
                used: used,
                available: available,
                usedP: (double) (used / (used + available)),
                hostname: hostname,
                mount: mount,
                ip: ip
        ]

        render result as JSON
    }
}
