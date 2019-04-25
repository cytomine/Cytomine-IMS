package be.cytomine.formats.supported

import be.cytomine.exception.FormatException

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

import grails.util.Holders
import utils.HttpUtils
import utils.MimeTypeUtils
import utils.PropertyUtils
import utils.ServerUtils
import utils.URLBuilder

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 22/04/14.
 */
class JPEG2000Format extends NativeFormat {

    public JPEG2000Format() {
        extensions = ["jp2"]
        mimeType = MimeTypeUtils.MIMETYPE_JP2
        iipUrl = Holders.config.cytomine.ims.jpeg2000.iip.url

        // https://sno.phy.queensu.ca/~phil/exiftool/TagNames/Jpeg2000.html
        cytominePropertyKeys[PropertyUtils.CYTO_WIDTH] = "Jpeg2000.ImageWidth"
        cytominePropertyKeys[PropertyUtils.CYTO_HEIGHT] = "Jpeg2000.ImageHeight"
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES] = "Jpeg2000.DisplayXResolution" // to check
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES] = "Jpeg2000.DisplayYResolution" // to check
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES_UNIT] = "Jpeg2000.DisplayXResolutionUnit" // to check
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES_UNIT] = "Jpeg2000.DisplayYResolutionUnit" // to check
        cytominePropertyKeys[PropertyUtils.CYTO_BPS] = "Jpeg2000.BitsPerComponent"
        cytominePropertyKeys[PropertyUtils.CYTO_SPP] = "Jpeg2000.NumberOfComponents"
        cytominePropertyKeys[PropertyUtils.CYTO_COLORSPACE] = "Jpeg2000.ColorSpace"
        cytominePropertyParsers[PropertyUtils.CYTO_BPS] = PropertyUtils.parseIntFirstWord
    }

    public boolean detect() {
        //I check the extension for the moment because did not find an another way
        boolean detect = extensions.any { it == this.file.extension() }
        if(detect && !Holders.config.cytomine.ims.jpeg2000.enabled)
            throw new FormatException("JPEG2000 disabled")

        return detect
    }

    @Override
    def associated() {
        return []
    }

    @Override
    BufferedImage associated(def label) {
        if (!label in associated())
            return null

        return null
    }

    @Override
    BufferedImage thumb(def params) {
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

    @Override
    String cropURL(def params) {
        int topLeftX = params.int('topLeftX')
        int topLeftY = params.int('topLeftY')
        double width = params.double('width')
        double height = params.double('height')
        double imageWidth = params.double('imageWidth')
        double imageHeight = params.double('imageHeight')

        def x = topLeftX / imageWidth
        def y = (imageHeight - topLeftY) / imageHeight
        double w = width / imageWidth
        double h = height / imageHeight

        if (x > 1 || y > 1)
            return null

        double computedWidth = width
        double computedHeight = height
        if (params.maxSize) {
            int maxSize = params.int('maxSize', 256)
            computedWidth = maxSize //Math.min(computedWidth, maxSize)
            computedHeight = maxSize //Math.min(computedHeight, maxSize)
        } else if (params.zoom) {
            int zoom = params.int('zoom', 0)
            computedWidth *= Math.pow(2, zoom)
            computedHeight *= Math.pow(2, zoom)
        }

        if (params.boolean("safe", true)) {
            int maxCropSize = new Integer(Holders.config.cytomine.ims.crop.maxSize)
            computedWidth = Math.min(computedWidth, maxCropSize)
            computedHeight = Math.min(computedHeight, maxCropSize)
        }

        /*
        Ruven P. (author of IIP Image)
        In fact, the region is calculated from the WID or HEI given, not from
        the full image size. So you get the requested region on the virtual
        750px resize. I guess you were expecting to get a region exactly of size
        WID?

        This is something that seems to have caused confusion with others also
        and perhaps the way it works in counter intuitive, so I'm considering
        changing the behaviour in the 1.0 release and have WID or HEI define the
        final region size rather than the virtual image size. In the meantime,
        the way to get around it is to calculate the appropriate WID that the
        full image would be. So if your image is x pixels wide, give WID the
        value of x/2 to get a 750px wide image.
        */
        if (width > computedWidth || height > computedHeight) {
            double tmpWidth = width
            double tmpHeight = height
            while (tmpWidth > computedWidth || tmpHeight > computedHeight) {
                tmpWidth = tmpWidth / 2
                tmpHeight = tmpHeight / 2
            }

            computedWidth = imageWidth / (width / tmpWidth)
            computedHeight = imageHeight / (height / tmpHeight)
        }

        def query = [
                FIF: this.file.absolutePath,
                WID: computedWidth,
                HEI: computedHeight,
                RGN: "$x,$y,$w,$h",
//                CNT: params.double("contrast"),
//                GAM: params.double("gamma"),
//                INV: params.boolean("inverse", false),
                BIT: /*Math.ceil((params.int("bits") ?: 8) / 8) **/ 8,
                QLT: params.int("jpegQuality", 99),
                CVT: params.format
        ]
        return HttpUtils.makeUrl(iipUrl, query)
    }

    @Override
    String tileURL(params) {
        if (params.tileGroup) {
            def tg = params.int("tileGroup")
            def z = params.int("z")
            def x = params.int("x")
            def y = params.int("y")
            def file = HttpUtils.encode(this.file.absolutePath)
            return "${iipUrl}?zoomify=${file}/TileGroup${tg}/${z}-${x}-${y}.jpg"
        }

        def z = params.int("z")
        def tileIndex = params.int("tileIndex")
        def query = [
                FIF: this.file.absolutePath,
//                CNT: params.double("contrast"),
//                GAM: params.double("gamma"),
//                INV: params.boolean("inverse", false),
                JTL: "$z,$tileIndex"
        ]

        return HttpUtils.makeUrl(iipUrl, query)
    }
}
