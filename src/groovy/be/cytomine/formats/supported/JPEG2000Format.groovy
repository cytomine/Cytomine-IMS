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
import utils.FilesUtils
import utils.HttpUtils
import utils.MimeTypeUtils
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
        iipUrls = ServerUtils.getServers(Holders.config.cytomine.iipImageServerJpeg2000)
    }

    public boolean detect() {
        //I check the extension for the moment because did not find an another way
        boolean detect = extensions.any { it == this.file.extension() }
        if(detect && !Holders.config.cytomine.Jpeg2000Enabled) throw new FormatException("JPEG2000 disabled");

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

        return thumb(256);
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

        return ImageIO.read(new URL(HttpUtils.makeUrl(ServerUtils.getServer(iipUrls), query)))
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
            int maxCropSize = new Integer(Holders.config.cytomine.maxCropSize)
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
        return HttpUtils.makeUrl(ServerUtils.getServer(iipUrls), query)
    }

    @Override
    String tileURL(params) {
        def server = ServerUtils.getServer(iipUrls)
        if (params.tileGroup) {
            def tg = params.int("tileGroup")
            def z = params.int("z")
            def x = params.int("x")
            def y = params.int("y")
            def file = HttpUtils.encode(this.file.absolutePath)
            return "${server}?zoomify=${file}/TileGroup${tg}/${z}-${x}-${y}.jpg"
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

        return HttpUtils.makeUrl(server, query)
    }

    public def properties() {
        def iipRequest = new URLBuilder(ServerUtils.getServer(iipURL))
        iipRequest.addParameter("FIF", this.file.absolutePath, true)
        iipRequest.addParameter("obj", "IIP,1.0")
        iipRequest.addParameter("obj", "Max-size")
        iipRequest.addParameter("obj", "Tile-size")
        iipRequest.addParameter("obj", "Resolution-number")
        iipRequest.addParameter("obj", "bits-per-channel")
        iipRequest.addParameter("obj", "colorspace")
        String propertiesURL = iipRequest.toString()
        String propertiesTextResponse = new URL(propertiesURL).text
        Integer width = null
        Integer height = null
        Integer depth = null
        String colorspace = null
        propertiesTextResponse.eachLine { line ->
            if (line.isEmpty()) return;

            def args = line.split(":")
            if (args.length != 2) return

            if (args[0].equals('Max-size')) {
                def sizes = args[1].split(' ')
                width = Integer.parseInt(sizes[0])
                height = Integer.parseInt(sizes[1])
            }

            if (args[0].equals('Bits-per-channel'))
                depth = Integer.parseInt(args[1])

            if (args[0].contains('Colorspace')) {
                def tokens = args[1].split(' ')
                if (tokens[2] == "1") {
                    colorspace = "grayscale"
                } else if (tokens[2] == "3") {
                    colorspace = "rgb"
                } else {
                    colorspace = "cielab"
                }
            }
        }
        assert(width)
        assert(height)
        def properties = [[key : "mimeType", value : mimeType]]
        properties << [ key : "cytomine.width", value : width ]
        properties << [ key : "cytomine.height", value : height ]
        properties << [ key : "cytomine.resolution", value : null ]
        properties << [ key : "cytomine.magnification", value : null ]
        properties << [ key : "cytomine.bitdepth", value : depth]
        properties << [ key : "cytomine.colorspace", value: colorspace]
        return properties
    }
}
