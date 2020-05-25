package be.cytomine.formats.heavyconvertable

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

import be.cytomine.exception.ConversionException
import be.cytomine.exception.MiddlewareException
import be.cytomine.formats.NotNativeFormat
import be.cytomine.formats.tools.CytomineFile
import grails.converters.JSON
import grails.util.Holders
import groovy.json.JsonOutput
import groovy.util.logging.Log4j
import utils.PropertyUtils

@Log4j
abstract class BioFormatConvertable extends NotNativeFormat implements IHeavyConvertableImageFormat {

    BioFormatConvertable() {
        cytominePropertyKeys[PropertyUtils.CYTO_WIDTH] = "Bioformats.Pixels.SizeX"
        cytominePropertyKeys[PropertyUtils.CYTO_HEIGHT] = "Bioformats.Pixels.SizeY"
        cytominePropertyKeys[PropertyUtils.CYTO_DEPTH] = "Bioformats.Pixels.SizeZ"
        cytominePropertyKeys[PropertyUtils.CYTO_CHANNELS] = "Bioformats.Pixels.SizeC"
        cytominePropertyKeys[PropertyUtils.CYTO_DURATION] = "Bioformats.Pixels.SizeT"
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES] = "Bioformats.Pixels.PhysicalSizeX"
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES] = "Bioformats.Pixels.PhysicalSizeY"
        cytominePropertyKeys[PropertyUtils.CYTO_Z_RES] = "Bioformats.Pixels.PhysicalSizeZ"
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES_UNIT] = "Bioformats.Pixels.PhysicalSizeXUnit"
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES_UNIT] = "Bioformats.Pixels.PhysicalSizeYUnit"
        cytominePropertyKeys[PropertyUtils.CYTO_Z_RES_UNIT] = "Bioformats.Pixels.PhysicalSizeZUnit"
        cytominePropertyKeys[PropertyUtils.CYTO_FPS] = "Bioformats.Pixels.TimeIncrement" //TODO: unit
        cytominePropertyKeys[PropertyUtils.CYTO_BPS] = "Bioformats.Pixels.BitsPerPixel"
        cytominePropertyKeys[PropertyUtils.CYTO_SPP] = "Bioformats.Pixels.SamplesPerPixel"
        cytominePropertyKeys[PropertyUtils.CYTO_MAGNIFICATION] = "Bioformats.Objective.NominalMagnification"
        cytominePropertyKeys[PropertyUtils.CYTO_COLORSPACE] = "" //TODO
        cytominePropertyKeys[PropertyUtils.CYTO_CHANNEL_NAMES] = "Bioformats.Channels.Name"
    }

    def makeRequest(def message) {
        if (!(Holders.config.cytomine.ims.conversion.bioformats.enabled as Boolean))
            throw new MiddlewareException("Convertor BioFormat not enabled")

        String hostName = Holders.config.cytomine.ims.conversion.bioformats.hostname
        int portNumber = Holders.config.cytomine.ims.conversion.bioformats.port as Integer
        try {
            log.info("BioFormats called on $hostName:$portNumber")
            Socket echoSocket = new Socket(hostName, portNumber)
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true)
            BufferedReader inp = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()))

            out.println(JsonOutput.toJson(message))
            String result = inp.readLine()
            return JSON.parse(result)
        } catch (UnknownHostException e) {
            throw new MiddlewareException(e.getMessage())
        }
    }

    @Override
    def convert() {
        def message = [
                path            : this.file.absolutePath,
                group           : this.group,
                onlyBiggestSerie: this.onlyBiggestSerie,
                action          : "convert"
        ]

        def response = makeRequest(message)
        def files = response.files
        def error = response.error

        log.info("BioFormats returned ${files?.size()} files")

        if ((files == [] || files == null) && error != null) {
            throw new ConversionException("BioFormats Exception : $error")
        }
        return files.collect { new CytomineFile(it.path as String, it.c, it.z, it.t, it.channelName) }
    }

    def properties() {
        def properties = super.properties()

        def message = [
                path: this.file.absolutePath,
                action: "properties",
                includeRawProperties: this.includeRawProperties()
        ]

        def response = makeRequest(message)
        if (response == null || response.error != null) {
            throw new MiddlewareException("BioFormats Exception : ${response?.error}")
        }

        properties += response
        return properties
    }

    abstract boolean getGroup();

    abstract boolean getOnlyBiggestSerie();

    abstract boolean includeRawProperties()
}
