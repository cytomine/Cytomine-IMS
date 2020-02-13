package be.cytomine.formats.supported.digitalpathology

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

import be.cytomine.formats.supported.NativeFormat
import be.cytomine.formats.tools.CustomExtensionFormat
import grails.util.Holders
import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.web.util.TypeConvertingMap
import org.openslide.AssociatedImage
import org.openslide.OpenSlide
import utils.PropertyUtils

import java.awt.image.BufferedImage

@Log4j
abstract class OpenSlideFormat extends NativeFormat implements CustomExtensionFormat {

    String vendor = null
    String customExtension = null

    protected OpenSlideFormat() {
        iipUrl = Holders.config.cytomine.ims.openslide.iip.url
        cytominePropertyKeys << ["cytomine.width": "openslide.level[0].width"]
        cytominePropertyKeys << ["cytomine.height": "openslide.level[0].height"]
        cytominePropertyKeys << ["cytomine.physicalSizeX": "openslide.mpp-x"]
        cytominePropertyKeys << ["cytomine.physicalSizeY": "openslide.mpp-y"]
        cytominePropertyKeys << ["cytomine.magnification": "openslide.objective-power"]
    }

    String tileURL(TypeConvertingMap params, File actualFile = null) {
        return super.tileURL(params, this.rename())
    }

    String cropURL(TypeConvertingMap params, File actualFile = null) {
        return super.cropURL(params, this.rename())
    }

    def associated() {
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
    }

    @Override
    BufferedImage thumb(TypeConvertingMap params) {
        // TODO - currently does not support: inverse, contrast, gamma (is it required for a thumb ?)
        BufferedImage thumbnail = null
        if (this.file.canRead()) {
            OpenSlide openSlide = new OpenSlide(this.file)
            def w = openSlide.getLevel0Width()
            def h = openSlide.getLevel0Height()
            thumbnail = openSlide.createThumbnailImage(0, 0, w, h, (int) params.maxSize, BufferedImage.TYPE_INT_ARGB_PRE)
            openSlide.close()
        }
        return thumbnail
    }

    def properties() {
        def properties = [:]
        if (!this.file.canRead()) {
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
        properties << [(PropertyUtils.CYTO_Y_RES_UNIT): "um"]

        return properties
    }

    def cytomineProperties() {
        def properties = super.cytomineProperties()

        properties << ["cytomine.bitPerSample": 8] //https://github.com/openslide/openslide/issues/41 (Hamamatsu)
        properties << ["cytomine.samplePerPixel": 3] //https://github.com/openslide/openslide/issues/42 (Leica, Mirax, Hamamatsu)
        properties << ["cytomine.colorspace": "rgb"]

        return properties
    }
}
