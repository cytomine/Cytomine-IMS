package be.cytomine.storage

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
import grails.converters.JSON
import grails.util.Holders
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

/**
 * Cytomine @ GIGA-ULG
 * User: lrollus
 * Date: 16/09/13
 * Time: 12:25
 */
@RestApi(name = "upload services", description = "Methods for uploading images")
class StorageController {

    def deployImagesService
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
            Cytomine cytomine = new Cytomine((String) cytomineUrl, (String) user.publicKey, (String) user.privateKey)

            cytomine.testHostConnection();

            def idStorage = Integer.parseInt(params['idStorage'] + "")
            def projects = []
            if (params['idProject']) {
                try {
                    projects << Integer.parseInt(params['idProject'] + "")
                } catch (NumberFormatException e) {
                    log.error "Integer parse Exception : "+params['idProject']
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
            log.info "filePath=$filePath"
            log.info "contentType=$contentType"
            long timestamp = new Date().getTime()

            def responseContent = uploadService.upload(cytomine, filename, idStorage, contentType, filePath, projects, currentUserId, properties, timestamp, isSync);

            render responseContent as JSON
        } catch (Exception e) {
            log.error e.toString()
            e.printStackTrace()
            response.status = 400;
            render e
            return
        }
    }

    @RestApiMethod(description="Method for getting used and free space of the image storage")
    def size () {

        def result = [:]

        String storagePath = Holders.config.cytomine.storagePath
        def proc = "df $storagePath".execute()
        proc.waitFor()

        String[] out = proc.text.split("\n")[1].trim().replaceAll("\\s+"," ").split(" ")

        boolean nfs;
        if(out[0].contains(":")) nfs = true;

        Long used = Long.parseLong(out[2])
        Long available = Long.parseLong(out[3])
        result.put("used",used)
        result.put("available",available)
        result.put("usedP",(double)(used/(used+available)))
        String hostname = ""
        String mount = ""

        if(nfs){
            hostname = out[0].split(":")[0]
            mount= out[0].split(":")[1]
        } else {
            hostname = "hostname".execute().text.split("\n")[0]
            mount = out[5]
        }

        result.put("hostname",hostname.hashCode())
        result.put("mount",mount)

        String ip = "host $hostname".execute().text
        if(ip.contains("not found")) {
            ip = null
        }
        else {
            ip = ip.split(" ").last().hashCode()
        }
        result.put("ip",ip)

        render result as JSON

    }
}
