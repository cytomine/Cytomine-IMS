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
import org.codehaus.groovy.grails.web.util.TypeConvertingMap
import utils.HttpUtils
import utils.ImageUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

@Log4j
abstract class NativeFormat extends Format {

    public String iipUrl

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
    BufferedImage thumb(TypeConvertingMap params) {
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
    String cropURL(TypeConvertingMap params) {
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

        def computedDimensions = ImageUtils.getComputedDimensions(params)

        def query = [
                FIF: this.file.absolutePath,
                WID: computedDimensions.computedWidth,
                HEI: computedDimensions.computedHeight,
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
    String tileURL(TypeConvertingMap params, File actualFile = null) {
        def file = actualFile ?: this.file

        if (params.tileGroup) {
            def tg = params.int("tileGroup") ?: Integer.parseInt(params.tileGroup.toLowerCase().replace("tilegroup", ""))
            def z = params.int("z")
            def x = params.int("x")
            def y = params.int("y")
            def filename = HttpUtils.encode(file.absolutePath)

            if (filename.endsWith("/"))
                filename = filename.substring(0, filename.length()-1)

            return "${iipUrl}?zoomify=${filename}/TileGroup${tg}/${z}-${x}-${y}.jpg"
        }

        def z = params.int("z")
        def tileIndex = params.int("tileIndex")
        def query = [
                FIF: file.absolutePath,
                CNT: params.double("contrast"),
                GAM: params.double("gamma"),
                INV: params.boolean("inverse") ?: null,
                MINMAX: params.minmax?.split("\\|"),
                JTL: "$z,$tileIndex"
        ]

        return HttpUtils.makeUrl(iipUrl, query)
    }
}
