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
import grails.util.Holders
import groovy.util.logging.Log4j
import org.apache.http.HttpResponse
import org.apache.http.client.HttpClient
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.codehaus.groovy.grails.web.util.TypeConvertingMap
import utils.HttpUtils
import utils.MimeTypeUtils
import utils.PropertyUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

@Log4j
class ISyntaxFormat extends NativeFormat implements CustomExtensionFormat {

    String vendor = "philips"
    String customExtension = "isyntax"

    ISyntaxFormat() {
        extensions = ["isyntax"]
        mimeType = MimeTypeUtils.MIMETYPE_ISYNTAX
        iipUrl = Holders.config.cytomine.ims.pims.url //TODO

        //TODO voir les clé données par PIMS metadata
        cytominePropertyKeys << ["cytomine.width": "openslide.level[0].width"] //TODO
        cytominePropertyKeys << ["cytomine.height": "openslide.level[0].height"] //TODO
        cytominePropertyKeys << ["cytomine.physicalSizeX": "openslide.mpp-x"] //TODO
        cytominePropertyKeys << ["cytomine.physicalSizeY": "openslide.mpp-y"] //TODO
        cytominePropertyKeys << ["cytomine.magnification": "openslide.objective-power"] //TODO
    }

    String tileURL(TypeConvertingMap params, File actualFile = null) {
        //si fichier visualisation.ISYNTAX existe pas, je le crée
        //TODO dont use the super fonction but overwrite it with pims call
        return super.tileURL(params, this.rename())
    }

    String cropURL(TypeConvertingMap params, File actualFile = null) {
        //si fichier visualisation.ISYNTAX existe pas, je le crée
        return super.cropURL(params, this.rename())
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
                FIF: this.file.absolutePath,
                WID: params.int("maxSize"),
                HEI: params.int("maxSize"),
//                INV: params.boolean("inverse", false),
//                CNT: params.double("contrast"),
//                GAM: params.double("gamma"),
                BIT: /*Math.ceil(((Integer) params.bits ?: 8) / 8) **/ 8,
                QLT: (params.format == "jpg") ? 99 : null,
                CVT: params.format
        ]

        return ImageIO.read(new URL(HttpUtils.makeUrl(iipUrl, query)))
    }

    def properties() {
        def properties = [:]
        //TODO call PIMS url.


        HttpClient httpclient = new DefaultHttpClient();

        HttpGet httpGet = new HttpGet("${iipUrl}/image/${file.path}/metadata");

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
}
