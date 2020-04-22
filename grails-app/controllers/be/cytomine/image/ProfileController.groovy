package be.cytomine.image

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

import be.cytomine.client.Cytomine
import be.cytomine.client.CytomineConnection
import be.cytomine.exception.AuthenticationException
import be.cytomine.exception.DeploymentException
import be.cytomine.exception.WrongParameterException
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.Point
import com.vividsolutions.jts.geom.Polygon
import com.vividsolutions.jts.io.ParseException
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

import java.awt.image.BufferedImage

class ProfileController extends ImageResponseController {

    def profileService

    @RestApiMethod(description = "Compute profile for an image")
    @RestApiParams(params = [
            @RestApiParam(name="core", type = "String", paramType = RestApiParamType.QUERY, description = "URL of linked Cytomine-core"),
            @RestApiParam(name = "user", type = "Long", paramType = RestApiParamType.QUERY, description = "The user id"),
            @RestApiParam(name = "uploadedFileParent", type = "Long", paramType = RestApiParamType.QUERY, description = "The uploaded file parent id"),
            @RestApiParam(name = "abstractImage", type = "Long", paramType = RestApiParamType.QUERY, description = "The abstract image id"),
    ])
    def computeProfile() {
        String coreURL = params.core
        String ISPublicKey = grailsApplication.config.cytomine.ims.server.publicKey
        String ISPrivateKey = grailsApplication.config.cytomine.ims.server.privateKey
        Cytomine.connection(coreURL, ISPublicKey, ISPrivateKey, true)

        // Check user authentication
//        def authorization = cytomineService.getAuthorizationFromRequest(request)
//        def messageToSign = cytomineService.getMessageToSignFromRequest(request)

        def keys = Cytomine.getInstance().getKeys(params.long("user")) // TODO: should use authorization instead of user id.
        if (!keys)
            throw new AuthenticationException("Auth failed: User not found! May be ImageServer user is not an admin!")

//        if (!cytomineService.testSignature(keys.get('privateKey'), authorization.signature, messageToSign))
//            throw new AuthenticationException("Auth failed.")

        CytomineConnection userConnection = new CytomineConnection(coreURL, (String) keys.get('publicKey'), (String) keys.get('privateKey'))
        def responseContent = [:]
        try {
            responseContent.status = 200
            def result = profileService.create(userConnection, params.long("uploadedFileParent"), params.long("abstractImage"))

            responseContent.uploadedFile = result.uploadedFile.getAttr()
            responseContent.companionFile = result.companionFile.getAttr()

        }
        catch(DeploymentException e){
            response.status = 500
            responseContent.status = 500
            responseContent.error = e.getMessage()
        }

        render responseContent as JSON
    }

    @RestApiMethod(description="Get the profile of a 3D image for a given location", extensions = ["json"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType= RestApiParamType.QUERY, description="The absolute path of the profile", required=true),
            @RestApiParam(name="location", type="String", paramType= RestApiParamType.QUERY, description="A geometry in WKT Format (Well-known text) in a cartesian coordinate system. Only POINT and POLYGON describing a rectangle are supported.", required=true),
            @RestApiParam(name="minSlice", type="int", paramType=RestApiParamType.QUERY, description="The minimum slice index", required=false),
            @RestApiParam(name="maxSlice", type="int", paramType=RestApiParamType.QUERY, description="The maximum slice index", required=false),
    ])
    def extractProfile() {
        String fif = URLDecoder.decode(params.fif, "UTF-8")
        Geometry geometry = sanitizeGeometry(params)
        def bounds = sanitizeBounds(params)

        def response
        if (geometry instanceof Point) {
            Point point = (Point) geometry
            response = profileService.pointProfile(fif, (int) point.getX(), (int) point.getY(), bounds)
        }
        else {
            response = profileService.geometryProfile(fif, geometry, bounds)
        }

        render response as JSON
    }

    @RestApiMethod(description="Get the statistics (min, max, avg) of the profile of a 3D image for a given location", extensions = ["json"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType= RestApiParamType.QUERY, description="The absolute path of the profile", required=true),
            @RestApiParam(name="location", type="String", paramType= RestApiParamType.QUERY, description="A geometry in WKT Format (Well-known text) in a cartesian coordinate system. Only POINT and POLYGON describing a rectangle are supported.", required=true),
            @RestApiParam(name="minSlice", type="int", paramType=RestApiParamType.QUERY, description="The minimum slice index", required=false),
            @RestApiParam(name="maxSlice", type="int", paramType=RestApiParamType.QUERY, description="The maximum slice index", required=false),
    ])
    def statsProfile() {
        String fif = URLDecoder.decode(params.fif, "UTF-8")
        Geometry geometry = sanitizeGeometry(params)
        def bounds = sanitizeBounds(params)

        def response
        if (geometry instanceof Point) {
            Point point = (Point) geometry
            response = profileService.pointProfileStats(
                    profileService.pointProfile(fif, (int) point.getX(), (int) point.getY(), bounds))
        }
        else {
            response = profileService.geometryProfileStats(profileService.geometryProfile(fif, geometry, bounds))
        }

        render response as JSON
    }

    def minProjection() {
        forward(action: "projection", params: [projection: 'min'])
    }

    def maxProjection() {
        forward(action: "projection", params: [projection: 'max'])
    }

    def averageProjection() {
        forward(action: "projection", params: [projection: 'average'])
    }

    def projection() {
        String fif = URLDecoder.decode(params.fif, "UTF-8")
        Geometry geometry = sanitizeGeometry(params)
        if (!(geometry instanceof Polygon)) {
            throw new WrongParameterException("Location is not a valid geometry.")
        }

        def bounds = sanitizeBounds(params)
        def projectionMode = params.projection
        BufferedImage image = profileService.polygonProfileProjection(fif, geometry, bounds, projectionMode)

        withFormat {
            png { responseBufferedImagePNG(image) }
            jpg { responseBufferedImageJPG(image) }
            tiff { responseBufferedImageTIFF(image) }
        }
    }

    private def sanitizeGeometry(params) {
        Geometry geometry
        try {
            geometry = new WKTReader().read(params.location as String)
        }
        catch (ParseException e) {
            throw new WrongParameterException("Location is not a valid WKT: ${e.getMessage()}")
        }

        if (!geometry.isValid()) {
            throw new WrongParameterException("Location is not a valid geometry.")
        }

        geometry
    }

    private def sanitizeBounds(params) {
        [
                min: params.int("minSlice", 0),
                max: params.int("maxSlice", Integer.MAX_VALUE)
        ]
    }
}
