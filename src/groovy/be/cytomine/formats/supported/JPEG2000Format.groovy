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
import utils.ServerUtils
import utils.URLBuilder

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 22/04/14.
 */
class JPEG2000Format extends SupportedImageFormat {

    public JPEG2000Format() {
        extensions = ["jp2"]
        mimeType = "image/jp2"
        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerJpeg2000)
    }

    public boolean detect() {
        //I check the extension for the moment because did not find an another way
        boolean detect = FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase() == "jp2"
        if(detect && !Holders.config.cytomine.Jpeg2000Enabled) throw new FormatException("JPEG2000 disabled");

        return detect
    }

    @Override
    BufferedImage associated(String label) {
        return thumb(256);
    }

    public BufferedImage thumb(int maxSize, def params=null) {
        def iipRequest = new URLBuilder(ServerUtils.getServer(iipURL))
        iipRequest.addParameter("FIF", absoluteFilePath, true)
        iipRequest.addParameter("HEI", "$maxSize")
        iipRequest.addParameter("WID", "$maxSize")
        iipRequest.addParameter("QLT", "99")
        iipRequest.addParameter("CVT", "jpeg")
        String thumbURL = iipRequest.toString()
        println thumbURL
        return ImageIO.read(new URL(thumbURL))
    }

    public def properties() {
        def iipRequest = new URLBuilder(ServerUtils.getServer(iipURL))
        iipRequest.addParameter("FIF", absoluteFilePath, true)
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

    String cropURL(def params, def charset = "UTF-8") {
        int topLeftX = params.int('topLeftX')
        int topLeftY = params.int('topLeftY')
        double width = params.double('width')
        double height = params.double('height')
        double imageWidth = params.double('imageWidth')
        double imageHeight = params.double('imageHeight')
//        boolean inverse = params.boolean("inverse", false)

        //All values x,y,w & h should be in ratios 0-1.0 [RGN=x,y,w,h]
        def x = (topLeftX == 0) ? 0 : 1 / (imageWidth / topLeftX)
        def y = ((imageHeight - topLeftY) == 0) ? 0 : 1 / (imageHeight / (imageHeight - topLeftY))
        double w = (width == 0) ? 0d : 1d / (imageWidth / width)
        double h = (height == 0) ? 0d : 1d / (imageHeight / height)

        if (x > 1 || y > 1) return ""

        int maxWidthOrHeight = new Integer(Holders.config.cytomine.maxCropSize)
        if (params.maxSize) {
            int maxSize = params.int('maxSize', 256)
            if (maxWidthOrHeight > maxSize) {
                maxWidthOrHeight = maxSize
            }
        }

        def iipRequest = new URLBuilder(ServerUtils.getServer(iipURL), charset)
        iipRequest.addParameter("FIF", params.fif, true)
        iipRequest.addParameter("RGN", "$x,$y,$w,$h")

        if (width > maxWidthOrHeight || height > maxWidthOrHeight) {
            int tmpWidth = width
            int tmpHeight = height
            while (tmpWidth > maxWidthOrHeight || tmpHeight > maxWidthOrHeight) {
                tmpWidth = tmpWidth / 2
                tmpHeight = tmpHeight / 2
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
            int hei = imageHeight / (height / tmpHeight)
            iipRequest.addParameter("HEI", "$hei")
        }
        if (params.contrast) iipRequest.addParameter("CNT", "$params.contrast")
        if (params.gamma) iipRequest.addParameter("GAM", "$params.gamma")
//        if (params.colormap) iipRequest.addParameter("CMP", params.colormap, true)
//        if (inverse) iipRequest.addParameter("INV", "true")
//        if (params.bits) {
//            def bits= params.int("bits", 8)
//            if (bits > 16) iipRequest.addParameter("BIT", 32)
//            else if (bits > 8) iipRequest.addParameter("BIT", 16)
//            else iipRequest.addParameter("BIT", 8)
//        }
        iipRequest.addParameter("CVT", params.format)
        return iipRequest.toString()
    }
}
