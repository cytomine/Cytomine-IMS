package be.cytomine.formats.supported

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

import be.cytomine.formats.Format
import grails.util.Holders
import groovy.util.logging.Log4j
import utils.HttpUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

@Log4j
abstract class NativeFormat extends Format {

    public String iipUrl

    /**
     * Get the list of available associated labels.
     * @return List<String>  the labels
     */
    abstract def associated()

    /**
     * Get an image associated to a label.
     * @param label The label
     * @return A buffered image for this associated file
     */
    abstract BufferedImage associated(def label)

    /**
     * Get an image thumb.
     * @param params
     *      - size The thumb size
     *      - inverse (optional, default: false)
     *      - contrast (optional, default: null)
     *      - gamma (optional, default: null)
     *      - bits (optional, default: 8)
     * @return
     */
    BufferedImage thumb(def params) {
        def query = [
                FIF: this.file.absolutePath,
                WID: params.int("maxSize"),
                HEI: params.int("maxSize"),
                INV: params.boolean("inverse") ?: null,
                CNT: params.double("contrast"),
                GAM: params.double("gamma"),
                BIT: (Integer) Math.ceil((params.int("bits") ?: 8) / 8) * 8,
                QLT: (params.format == "jpg") ? 99 : null,
                CVT: params.format
        ]

        def url = HttpUtils.makeUrl(iipUrl, query)
        log.info(url)
        return ImageIO.read(new URL(url))
    }

    /**
     * Get the IIP url for a crop.
     * @param params
     *      - topLeftX
     *      - topLeftY
     *      - width
     *      - height
     *      - imageWidth
     *      - imageHeight
     *      - maxSize (optional, default: 256)
     *      - zoom (optional, default: 0)
     *      - safe (optional, default: true)
     *      - contrast (optional, default: null)
     *      - gamma (optional, default: null)
     *      - bits (optional, default: 8)
     *      - inverse (optional, default: false)
     *      - jpegQuality (optional, default: 99)
     * @return
     */
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

        def query = [
                FIF: this.file.absolutePath,
                WID: computedWidth,
                HEI: computedHeight,
                RGN: "$x,$y,$w,$h",
                CNT: params.double("contrast"),
                GAM: params.double("gamma"),
                INV: params.boolean("inverse") ?: null,
                BIT: (Integer) Math.ceil((params.int("bits") ?: 8) / 8) * 8,
                QLT: params.int("jpegQuality", 99),
                CVT: params.format
        ]
        return HttpUtils.makeUrl(iipUrl, query)
    }

    /**
     * Get the IIP url for a tile.
     * @param params
     *      - tileGroup (optional, if set use Zoomify protocol)
     *      - z
     *      - x (optional, used in Zoomify protocol)
     *      - y (optional, used in Zoomify protocol)
     *      - tileIndex (optional, used in JTL protocol)
     *      - contrast (optional, used in JTL protocol, default: null)
     *      - gamma (optional, used in JTL protocol, default: null)
     *      - inverse (optional, used in JTL protocol, default: false)
     * @return
     */
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
                CNT: params.double("contrast"),
                GAM: params.double("gamma"),
                INV: params.boolean("inverse") ?: null,
                JTL: "$z,$tileIndex"
        ]

        return HttpUtils.makeUrl(iipUrl, query)
    }
}
