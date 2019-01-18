package be.cytomine.formats.supported

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

import be.cytomine.formats.Format
import grails.util.Holders
import utils.ServerUtils
import utils.URLBuilder

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 10/05/14.
 */
abstract class SupportedImageFormat extends Format {

    public String[] extensions = null
    public String mimeType = null
    public String widthProperty = "width"
    public String heightProperty = "height"
    public String resolutionProperty = "resolution"
    public String magnificiationProperty = "magnificiation"
    public List<String> iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerCyto)

    abstract BufferedImage associated(String label)

    abstract BufferedImage thumb(int maxSize, def params=null)

    String[] associated() {
        return ["macro"]
    }

    def properties() {
        BufferedImage bufferedImage = ImageIO.read(new File(absoluteFilePath))
        def properties = [[key: "mimeType", value: mimeType]]
        properties << [key: "cytomine.width", value: bufferedImage.getWidth()]
        properties << [key: "cytomine.height", value: bufferedImage.getHeight()]
        properties << [key: "cytomine.resolution", value: null]
        properties << [key: "cytomine.magnification", value: null]
        properties << [key: "cytomine.bitdepth", value: 8]
        properties << [key: "cytomine.colorspace", value: null]
        return properties
    }

    String cropURL(def params, def charset = "UTF-8") {
        int topLeftX = params.int('topLeftX')
        int topLeftY = params.int('topLeftY')
        double width = params.double('width')
        double height = params.double('height')
        double imageWidth = params.double('imageWidth')
        double imageHeight = params.double('imageHeight')
        boolean inverse = params.boolean("inverse", false)

        def x = topLeftX/imageWidth
        def y = (imageHeight - topLeftY)/imageHeight
        double w = width/imageWidth
        double h = height/imageHeight

        if (x > 1 || y > 1) return ""

        double computedWidth = width
        double computedHeight = height
        if (params.maxSize) {
            int maxSize = params.int('maxSize', 256)
            computedWidth = Math.min(computedWidth, maxSize)
            computedHeight = Math.min(computedHeight, maxSize)
        } else if (params.zoom) {
            int zoom = params.int('zoom', 0)
            computedWidth /= Math.pow(2, zoom)
            computedHeight /= Math.pow(2, zoom)
        }

        if (params.safe) {
            int maxCropSize = new Integer(Holders.config.cytomine.maxCropSize)
            computedWidth = Math.min(computedWidth, maxCropSize)
            computedHeight = Math.min(computedHeight, maxCropSize)
        }

        def iipRequest = new URLBuilder(ServerUtils.getServer(iipURL), charset)
        iipRequest.addParameter("FIF", params.fif, true)
        iipRequest.addParameter("RGN", "$x,$y,$w,$h")
        iipRequest.addParameter("HEI", "$computedHeight")
        iipRequest.addParameter("WID", "$computedWidth")
        if (params.contrast) iipRequest.addParameter("CNT", "$params.contrast")
        if (params.gamma) iipRequest.addParameter("GAM", "$params.gamma")
        if (params.colormap) iipRequest.addParameter("CMP", params.colormap, true)
        if (inverse) iipRequest.addParameter("INV", "true")
        if (params.bits) {
            def bits= params.int("bits", 8)
            if (bits > 16) iipRequest.addParameter("BIT", 32)
            else if (bits > 8) iipRequest.addParameter("BIT", 16)
            else iipRequest.addParameter("BIT", 8)
        }
        iipRequest.addParameter("CVT", params.format)
        return iipRequest.toString()
    }

    String tileURL(fif, params, with_zoomify) {
        if (with_zoomify) {
            return "${ServerUtils.getServer(iipURL)}?zoomify=" + URLEncoder.encode(fif, "UTF-8") +
                    "/TileGroup$params.tileGroup/$params.z-$params.x-$params.y" + ".jpg"
        }
        else {
            def inverse = params.boolean("inverse", false)

            def iipRequest = new URLBuilder(ServerUtils.getServer(iipURL))
            iipRequest.addParameter("FIF", fif, true)
            if (params.contrast) iipRequest.addParameter("CNT", params.contrast)
            if (params.gamma) iipRequest.addParameter("GAM", params.gamma)
            if (params.colormap) iipRequest.addParameter("CMP", params.colormap, true)
            if (inverse) iipRequest.addParameter("INV", "true")

            iipRequest.addParameter("JTL", "$params.z,$params.tileIndex")
            return iipRequest.toString()
        }
    }

    // TODO do it with OpenSlide or IIP ?
    protected BufferedImage rotate90ToRight(BufferedImage inputImage) {
        int width = inputImage.getWidth()
        int height = inputImage.getHeight()
        BufferedImage returnImage = new BufferedImage(height, width, inputImage.getType())

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                returnImage.setRGB(height - y - 1, x, inputImage.getRGB(x, y))
            }
        }
        return returnImage
    }
}
