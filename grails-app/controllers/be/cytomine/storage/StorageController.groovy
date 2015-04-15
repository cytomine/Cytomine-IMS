package be.cytomine.storage

/*
 * Copyright (c) 2009-2015. Authors: see NOTICE file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import be.cytomine.client.Cytomine
import be.cytomine.client.models.Storage
import be.cytomine.client.models.UploadedFile
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.ImageFormat
import grails.converters.JSON
import org.apache.commons.io.FilenameUtils
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import org.springframework.web.multipart.commons.CommonsMultipartFile
import utils.FilesUtils

/**
 * Cytomine @ GIGA-ULG
 * User: lrollus
 * Date: 16/09/13
 * Time: 12:25
 */
@RestApi(name = "upload services", description = "Methods for uploading images")
class StorageController {

    def deployImagesService
    def backgroundService
    def uploadService
    def cytomineService

    @RestApiMethod(description="Method for uploading an image")
    @RestApiParams(params=[
            @RestApiParam(name="files[]", type="data", paramType = RestApiParamType.QUERY, description = "The files content (Multipart)"),
            @RestApiParam(name="cytomine", type="String", paramType = RestApiParamType.QUERY, description = "The url of Cytomine"),
            @RestApiParam(name="idStorage", type="int", paramType = RestApiParamType.QUERY, description = "The id of the targeted storage"),
            @RestApiParam(name="sync", type="boolean", paramType = RestApiParamType.QUERY, description = "Indicates if operations are done synchronously or not (false by default)", required = false),
            @RestApiParam(name="idProject", type="int", paramType = RestApiParamType.QUERY, description = " The id of the targeted project", required = false),
            @RestApiParam(name="keys", type="String", paramType = RestApiParamType.QUERY, description = "The keys of the properties you want to link with your files (e.g. : key1,key2, ...)", required = false),
            @RestApiParam(name="values", type="String", paramType = RestApiParamType.QUERY, description = "The values of the properties you want to link with your files (e.g. : key1,key2, ...)", required = false)
    ])
    def upload () {

        try {

            String cytomineUrl =  params['cytomine']//grailsApplication.config.grails.cytomineUrl
            String pubKey = grailsApplication.config.cytomine.imageServerPublicKey
            String privKey = grailsApplication.config.cytomine.imageServerPrivateKey

            log.info "Upload is made on Cytomine = $cytomineUrl"
            log.info "We use $pubKey/$privKey to connect"

            def user = cytomineService.tryAPIAuthentification(cytomineUrl,pubKey,privKey,request)
            long currentUserId = user.id

            log.info "init cytomine..."
            Cytomine cytomine = new Cytomine((String) cytomineUrl, (String) user.publicKey, (String) user.privateKey, "./")

            def idStorage = Integer.parseInt(params['idStorage'] + "")
            def projects = []
            if (params['idProject']) {
                try {
                    projects << Integer.parseInt(params['idProject'] + "")
                } catch (Exception e) {
                }
            }

            def properties = [:]
            def keys = []
            def values = []
            log.info "keys=" + params["keys"]
            log.info "values=" + params["values"]
            if(params["keys"]!=null && params["keys"]!="") {
                keys = params["keys"].split(",")
                values = params["values"].split(",")
            }
            if(keys.size()!=values.size()) {
                throw new Exception("Key.size <> Value.size!");
            }
            keys.eachWithIndex { key, index ->
                properties[key]=values[index];
            }

            boolean isSync = params.boolean('sync')
            log.info "sync="+isSync

            String filename = (String) params['files[].name']
            def filePath = (String) params['files[].path']
            String contentType = params['files[].content_type']

            log.info "idStorage=$idStorage"
            log.info "projects=$projects"
            log.info "filename=$filename"
            log.info "contentType=$contentType"
            long timestamp = new Date().getTime()

            def responseContent = uploadService.upload(cytomine, filename, idStorage, contentType, filePath, projects, currentUserId, properties, timestamp, isSync);

            render responseContent as JSON
        } catch (Exception e) {
            log.error e
            e.printStackTrace()
            response.status = 400;
            render e
            return
        }
    }





}
