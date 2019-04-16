package be.cytomine.formats.supported.digitalpathology

import be.cytomine.formats.detectors.OpenSlideDetector

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

import be.cytomine.formats.supported.NativeFormat
import org.openslide.AssociatedImage
import org.openslide.OpenSlide
import utils.ServerUtils
import utils.URLBuilder

import java.awt.image.BufferedImage

abstract class OpenSlideFormat extends NativeFormat /*implements OpenSlideDetector*/ {

    String vendor = null

    protected def cytominePropertyKeys = [
            "cytomine.width": "openslide.level[0].width",
            "cytomine.height": "openslide.level[0].height",
            "cytomine.physicalSizeX": "openslide.mpp-x",
            "cytomine.physicalSizeY": "openslide.mpp-y",
            "cytomine.magnification": "openslide.objective-power",
    ]

    public BufferedImage associated(String label) { //should be abstract
        File slideFile = this.file
        BufferedImage associatedBufferedImage = null
        if (slideFile.canRead()) {
            OpenSlide openSlide = new OpenSlide(slideFile)
            openSlide.getAssociatedImages().each {
                if (it.key == label) {
                    AssociatedImage associatedImage = it.value
                    associatedBufferedImage = associatedImage.toBufferedImage()
                }
            }
            openSlide.close()
        }
        return associatedBufferedImage
    }

    @Override
    BufferedImage thumb(Object params) {
        return null
    }

    @Override
    BufferedImage associated(Object label) {
        return null
    }

    @Override
    String associated() {
        return null
    }

    public def properties() {
        def properties = super.properties()
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
        catch(Exception e) {
            throw new Exception("Openslide is unable to read ${this.file}: ${e.getMessage()}")
        }

        cytominePropertyKeys.each { cytoKey, openslideKey ->
            if (!openslideKey || properties.hasProperty(openslideKey))
                return
            def value = properties.get(openslideKey)
            if (value) {
                properties << [(cytoKey): cytominePropertyParsers.get(cytoKey)(value)]
            }
        }

        properties << ["cytomine.bitPerSample": 8] //https://github.com/openslide/openslide/issues/41 (Hamamatsu)
        properties << ["cytomine.samplePerPixel": 3] //https://github.com/openslide/openslide/issues/42 (Leica, Mirax, Hamamatsu)
        properties << ["cytomine.colorspace": "rgb"]

        return properties
    }

    @Override
    String cropURL(Object params) {
        return null
    }

    @Override
    String tileURL(Object params) {
        return null
    }

    public BufferedImage thumb(int maxSize, def params=null) {
        OpenSlide openSlide = new OpenSlide(this.file)
        BufferedImage thumbnail = openSlide.createThumbnailImage(0, 0, openSlide.getLevel0Width(), openSlide.getLevel0Height(), maxSize, BufferedImage.TYPE_INT_ARGB_PRE)
        openSlide.close()
        return thumbnail
    }
}
