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
import be.cytomine.client.collections.Collection
import be.cytomine.client.models.Project
import be.cytomine.client.models.Storage
import be.cytomine.client.models.User
import be.cytomine.exception.AuthenticationException
import be.cytomine.exception.DeploymentException
import grails.converters.JSON
import grails.util.Holders
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

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
            String ISPublicKey = grailsApplication.config.cytomine.imageServerPublicKey
            String ISPrivateKey = grailsApplication.config.cytomine.imageServerPrivateKey
            log.info "Upload is made on Cytomine = $coreURL with image server $ISPublicKey/$ISPrivateKey key pair"
            CytomineConnection imsConnection = Cytomine.connection(coreURL, ISPublicKey, ISPrivateKey)

            // Check user authentication
            def authorization = cytomineService.getAuthorizationFromRequest(request)
            def messageToSign = cytomineService.getMessageToSignFromRequest(request)

            def keys = Cytomine.getInstance().getKeys(authorization.publicKey)
            log.info (keys.getAttr())
            if (!keys)
                throw new AuthenticationException("Auth failed: User not found! May be ImageServer user is not an admin!")

            if (!cytomineService.testSignature(keys.get('privateKey'), authorization.signature, messageToSign))
                throw new AuthenticationException("Auth failed.")

            CytomineConnection userConnection = Cytomine.connection(coreURL, (String) keys.get('publicKey'), (String) keys.get('privateKey'))
            def user = Cytomine.getInstance().getCurrentUser()

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

            def responseContent = [:]
            try {
                responseContent.status = 200;
                responseContent.name = filename
                def uploadResult = uploadService.upload(user as User, storage as Storage, filename, filePath, isSync, projects, properties)
//
                responseContent.uploadFile = uploadResult.uploadedFile
//                responseContent.images = uploadResult.images


            } catch(DeploymentException e){
                response.status = 500;
                responseContent.status = 500;
                responseContent.error = e.getMessage()
//                responseContent.files = [[name:filename, size:0, error:responseContent.error]]
            }

            responseContent = [responseContent]

            render responseContent as JSON
        }
        catch (CytomineException e) {
            log.error(e.toString())
            log.error(e.printStackTrace())
            response.status = e.getHttpCode()
            render e
        }
        catch (Exception e) {
            log.error(e.toString())
            log.error(e.printStackTrace())
            response.status = 400
            render e.getCause().toString()
        }
    }

    @RestApiMethod(description="Method for getting used and free space of the image storage")
    def size () {

        def result = [:]

        String storagePath = Holders.config.cytomine.storagePath
        def proc = "df $storagePath".execute()
        proc.waitFor()

        String[] out = proc.text.split("\n")[1].trim().replaceAll("\\s+", " ").split(" ")

        boolean nfs
        if (out[0].contains(":")) nfs = true

        Long used = Long.parseLong(out[2])
        Long available = Long.parseLong(out[3])
        result.put("used", used)
        result.put("available", available)
        result.put("usedP", (double) (used / (used + available)))
        String hostname = ""
        String mount = ""

        if (nfs) {
            hostname = out[0].split(":")[0]
            mount = out[0].split(":")[1]
        } else {
            hostname = "hostname".execute().text.split("\n")[0]
            mount = out[5]
        }

        result.put("hostname", hostname.hashCode())
        result.put("mount", mount)

        String ip = "host $hostname".execute().text
        if (ip.contains("not found")) {
            ip = null
        } else {
            ip = ip.split(" ").last().hashCode()
        }
        result.put("ip", ip)

        render result as JSON
    }
}
