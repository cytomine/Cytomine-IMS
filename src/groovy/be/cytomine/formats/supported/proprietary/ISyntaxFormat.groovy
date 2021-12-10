package be.cytomine.formats.supported.proprietary

/*
 * Copyright (c) 2009-2021. Authors: see NOTICE file.
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

import be.cytomine.exception.FormatException
import be.cytomine.formats.supported.NativeFormat
import be.cytomine.formats.tools.CustomExtensionFormat
import grails.converters.deep.JSON
import grails.util.Holders
import groovy.json.JsonBuilder
import groovy.util.logging.Log4j
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.util.EntityUtils
import org.codehaus.groovy.grails.web.util.TypeConvertingMap
import utils.HttpUtils
import utils.ImageUtils
import utils.MimeTypeUtils
import utils.PropertyUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException

@Log4j
class ISyntaxFormat extends NativeFormat implements CustomExtensionFormat {

    String vendor = "philips"
    String customExtension = "isyntax"

    ISyntaxFormat() {
        extensions = ["isyntax"]
        mimeType = MimeTypeUtils.MIMETYPE_ISYNTAX
        iipUrl = Holders.config.cytomine.ims.pims.url

        cytominePropertyKeys << ["cytomine.width": "width"]
        cytominePropertyKeys << ["cytomine.height": "height"]
        cytominePropertyKeys << ["cytomine.physicalSizeX": "physical_size_x"]
        cytominePropertyKeys << ["cytomine.physicalSizeY": "physical_size_y"]
        //cytominePropertyKeys << ["cytomine.magnification": "openslide.objective-power"] //TODO: info somwhere in pims?


//        http://localhost:5000/image/upload1638796095654515/1.isyntax/info/image
//        {"original_format":"ISYNTAX",
//            "width":25221,
//            "height":21125,
//            "depth":1,
//            "duration":1,
//            "physical_size_x":0.25,
//            "physical_size_y":0.25,
//            "physical_size_z":null,
//            "frame_rate":null,
//            "n_channels":3,
//            "n_intrinsic_channels":3,
//            "n_distinct_channels":1,
//            "n_planes":3,
//            "acquired_at":
//            "1970-08-22T16:30:12.231659+00:00",
//            "description":null,
//            "pixel_type":"uint8",
//            "significant_bits":8,
//            "n_samples_per_intrinsic_channel":1}


        //http://localhost:5000/image/upload1638796095654515/1.isyntax/metadata

//        {
//            "size":4,
//            "items":[
//                {
//                    "key":"date_of_last_calibration",
//                    "value":[
//                        "20190612"
//                ],
//                    "type":"LIST",
//                    "namespace":""
//                },
//                {
//                    "key":"time_of_last_calibration",
//                    "value":[
//                        "230908"
//                ],
//                    "type":"LIST",
//                    "namespace":""
//                },
//                {
//                    "key":"device_serial_number",
//                    "value":"FMT0132",
//                    "type":"STRING",
//                    "namespace":""
//                },
//                {
//                    "key":"lossy_image_compression_ratio",
//                    "value":15.0,
//                    "type":"DECIMAL",
//                    "namespace":""
//                }
//        ]
//        }

    }

    String tileURL(TypeConvertingMap params, File actualFile = null) {
        //TODO: Is this comment from RH still relevant with PIMS/isyntax:  "si fichier visualisation.ISYNTAX existe pas, je le crée"

//        * @param params
//        *      - tileGroup (optional, if set use Zoomify protocol)
//        *      - z
//                *      - x (optional, used in Zoomify protocol)
//                *      - y (optional, used in Zoomify protocol)
//                *      - tileIndex (optional, used in JTL protocol)
//                *      - contrast (optional, used in JTL protocol, default: null)
//                *      - gamma (optional, used in JTL protocol, default: null)
//                *      - inverse (optional, used in JTL protocol, default: false)


        //http://localhost:5000/image/{filepath}/tile/zoom/{zoom}/ti/{ti}{extension}

        String filepath = removePimsPathPrefix(params.fif);
        Integer zoom = params.int("z");
        String tileIndex = params.int("tileIndex")

        return HttpUtils.makeUrl(iipUrl + "/image/" + filepath + "/normalized-tile/zoom/" + zoom + "/ti/" + tileIndex + ".jpg", [:])
    }

    BufferedImage cropImage(TypeConvertingMap params, File actualFile = null) {
        //TODO: same as for tileURL "si fichier visualisation.ISYNTAX existe pas, je le crée"

        def parameters = [
                length: params.int('maxSize', 256),
                context_factor: params.double('increaseArea'),
                gammas: 1,
                annotations: [geometry: params.location],
                //channels: params.int('c'),
                //z_slices: params.int('z'),
                //timepoints: params.int('t'),
                colormaps: (params.colormap) ? (List) params.colormap.split(',') : null
        ]

        http://localhost:5000/image/{filepath}/annotation/crop{extension}

        String filepath = removePimsPathPrefix(params.fif)

        String server = iipUrl
        String url = "/image/$filepath/annotation/crop.jpg"
        def body = new JsonBuilder(parameters).toPrettyString()
        def http = new HTTPBuilder(server)

        println server
        println url
        println body
        BufferedImage response = (BufferedImage) http.post(path: url, requestContentType: "application/json", body: body) { response ->
            HttpEntity entity = response.getEntity()
            return ImageIO.read(entity.getContent())
        }
        return response
//        {
//            "height": 2,
//            "width": 2,
//            "length": 2,
//            "zoom": 0,
//            "level": 0,
//            "channels": 0,
//            "z_slices": 0,
//            "timepoints": 0,
//            "min_intensities": "AUTO_IMAGE",
//            "max_intensities": "AUTO_IMAGE",
//            "colormaps": "string",
//            "filters": "OTSU",
//            "gammas": 10,
//            "bits": "AUTO",
//            "colorspace": "GRAY",
//            "region": {
    //            "reference_tier_index": 0,
    //            "tier_index_type": "LEVEL",
    //            "top": 256,
    //            "left": 256,
    //            "width": 256,
    //            "height": 256
//        },
//            "annotations": [
//                {
//                    "geometry": "POINT(10 10)",
//                    "fill_color": "#FF00FF",
//                    "stroke_color": "string",
//                    "stroke_width": 0
//                }
//        ],
//            "annotation_style": {
//            "mode": "CROP",
//            "point_envelope_length": 100,
//            "point_cross": "CROSS",
//            "background_transparency": 0
//        }
//        }


//        FIF: file.absolutePath,
//        WID: computedDimensions.computedWidth,
//        HEI: computedDimensions.computedHeight,
//        RGN: "$x,$y,$w,$h",
//        CNT: params.double("contrast"),
//        GAM: params.double("gamma"),
//        INV: params.boolean("inverse") ?: null,
//        BIT: (Integer) Math.ceil((params.int("bits") ?: 8) / 8) * 8,
//        QLT: params.int("jpegQuality", 99),
//        CVT: params.format

    }

    //TODO ?
    /*def associated() {
        def labels = []
        if (this.file.canRead()) {
            labels = new OpenSlide(this.file).getAssociatedImages().collect { it.key }
        }
        return labels
    }

    BufferedImage associated(def label) {
        BufferedImage associated = null
        if (this.file.canRead()) {
            OpenSlide openSlide = new OpenSlide(this.file)
            openSlide.getAssociatedImages().each {
                if (it.key == label) {
                    AssociatedImage associatedImage = it.value
                    associated = associatedImage.toBufferedImage()
                }
            }
            openSlide.close()
        }
        return associated
    }*/

    @Override
    boolean detect() {
        //I check the extension for the moment because did not find an another way
        boolean detect = extensions.any { it == this.file.extension() }
        if (detect && !Holders.config.cytomine.ims.pims.enabled)
            throw new FormatException("PIMS disabled")

        return detect
    }

    //TODO
    @Override
    BufferedImage thumb(TypeConvertingMap params) {
        params.format = "jpg" //Only supported format by JPEG2000 IIP version
        def query = [
                height: params.int("maxSize"),
                width: params.int("maxSize")
//                FIF: this.file.absolutePath,
//                WID: params.int("maxSize"),
//                HEI: params.int("maxSize"),
////                INV: params.boolean("inverse", false),
////                CNT: params.double("contrast"),
////                GAM: params.double("gamma"),
//                BIT: /*Math.ceil(((Integer) params.bits ?: 8) / 8) **/ 8,
//                QLT: (params.format == "jpg") ? 99 : null,
//                CVT: params.format
        ]
        String filepath = removePimsPathPrefix(params.fif);

        // http://localhost:5000/image/{filepath}/thumb{extension}

        String url = HttpUtils.makeUrl(iipUrl + "/image/" + filepath + "/thumb." + params.format, query)
        return ImageIO.read(new URL(url))
    }

    def properties() {
        def properties = [:]
        //TODO call PIMS url.


        HttpClient httpclient = new DefaultHttpClient();

        HttpGet httpGet = new HttpGet("${iipUrl}/image/${file.path}/info/image");

        HttpResponse httpResponse = httpclient.execute(httpGet);
        InputStream instream = httpResponse.getEntity().getContent()



        //TODO get text, convert to JSON then fill the properties


        /*if (!this.file.canRead()) {
            throw new FileNotFoundException("Unable to read ${this.file}")
        }

        try {
            OpenSlide openSlide = new OpenSlide(this.file)
            openSlide.getProperties().each {
                properties << [(it.key): it.value]
            }
            openSlide.close()
        }
        catch (Exception e) {
            throw new Exception("Openslide is unable to read ${this.file}: ${e.getMessage()}")
        }

        properties << [(PropertyUtils.CYTO_X_RES_UNIT): "um"]
        properties << [(PropertyUtils.CYTO_Y_RES_UNIT): "um"]*/

        return properties
    }

    def cytomineProperties() {
        def properties = super.cytomineProperties()

        properties << [(PropertyUtils.CYTO_X_RES): 0.25]
        properties << [(PropertyUtils.CYTO_Y_RES): 0.25]
        properties << [(PropertyUtils.CYTO_MAGNIFICATION): 40]

        //TODO check
        properties << ["cytomine.bitPerSample": 8] //https://github.com/openslide/openslide/issues/41 (Hamamatsu)
        properties << ["cytomine.samplePerPixel": 3] //https://github.com/openslide/openslide/issues/42 (Leica, Mirax, Hamamatsu)
        properties << ["cytomine.colorspace": "rgb"]


        return properties
    }

    private def String removePimsPathPrefix(String path) {
        return path.replaceAll(Holders.config.cytomine.ims.pims.pathPrefix, "")
    }
}
