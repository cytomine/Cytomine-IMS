package be.cytomine.processing

import be.cytomine.image.ImageResponseController

/*
 * Copyright (c) 2009-2017. Authors: see NOTICE file.
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

import be.cytomine.processing.image.filters.Colour_Deconvolution
import be.cytomine.processing.merge.CytomineRGBStackMerge
import ij.IJ
import ij.ImagePlus
import ij.plugin.ContrastEnhancer
import ij.process.ImageConverter
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

import javax.imageio.ImageIO
import java.awt.*
import java.awt.image.BufferedImage

/**
 * Cytomine @ GIGA-ULG
 * User: stevben
 * Date: 1/06/11
 * Time: 13:44
 */
@RestApi(name = "vision services", description = "Methods for image processing services")
class VisionController extends ImageResponseController {

    def imageProcessingService
    def tileService

    @RestApiMethod(description="Merge multiple channel (multiple image) to an image", extensions = ["png"])
    @RestApiParams(params=[
            @RestApiParam(name="url0", type="String", paramType = RestApiParamType.QUERY, description = "URL for the tile of the first channel"),
            @RestApiParam(name="url...", type="String", paramType = RestApiParamType.QUERY, description = "URL for the tile of the ... channel"),
            @RestApiParam(name="urlN", type="String", paramType = RestApiParamType.QUERY, description = "URL for the tile of the N channel"),
            @RestApiParam(name="color0", type="String", paramType = RestApiParamType.QUERY, description = "Color for the tile of the first channel"),
            @RestApiParam(name="color...", type="String", paramType = RestApiParamType.QUERY, description = "Color for the tile of the ... channel"),
            @RestApiParam(name="colorN", type="String", paramType = RestApiParamType.QUERY, description = "Color for the tile of the N channel"),
            @RestApiParam(name="zoomify", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
            @RestApiParam(name="tileGroup", type="int", paramType = RestApiParamType.QUERY, description = "The Tile Group (see zoomify format)"),
            @RestApiParam(name="z", type="int", paramType = RestApiParamType.QUERY, description = "The Z index (see zoomify format)"),
            @RestApiParam(name="x", type="int", paramType = RestApiParamType.QUERY, description = "The X index (see zoomify format)"),
            @RestApiParam(name="y", type="int", paramType = RestApiParamType.QUERY, description = "The Y index (see zoomify format)")
    ])
    def merge() {

        def paramsWithoutUrl = params.clone()

        def urls = extractParams("url")
        int i = 0
        urls = urls.collect {
            paramsWithoutUrl.remove("url" + String.valueOf(i++))
            return it + "&"
        }
        def colors = extractParams("color")
        i = 0
        colors = colors.collect {
            paramsWithoutUrl.remove("color" + String.valueOf(i++))
            int intValue = Integer.parseInt(it, 16);
            return new Color(intValue);
        }

        paramsWithoutUrl.remove("zoomify")

        log.info "paramsWithoutUrl=$paramsWithoutUrl"
        log.info "Urls=$urls"
        log.info "Colors=$colors"

        def postParam = paramsWithoutUrl.collect { it.key + "=" + it.value }.join("&")

        if (urls.isEmpty()) {
            render "url argument is missing (start with url0=)!"
            response.status = 400
        }
        else {
            log.info "urls=$urls"

            ImagePlus[] images = new ImagePlus[urls.size()]
            urls.eachWithIndex { url, index ->
                log.info "load=${url + postParam}"
                images[index] = new ImagePlus("Image$index", (java.awt.Image) ImageIO.read(new URL(url + "&" + postParam)))
            }

            Color[] colorsArray = colors.toArray(new Color[colors.size()])
            ImagePlus result = CytomineRGBStackMerge.merge(images, colorsArray, false)

            BufferedImage resultImage = result.getBufferedImage()
            responseBufferedImage(resultImage)
        }
    }


    def process() {
        if (!request.queryString) return
        def split = request.queryString.split("url=")
        Boolean cytomineAuthenticationRequired = params.boolean('cytomine_auth')
        String imageURL = "/images/notavailable.jpg"
        if (split.size() > 0) {
            imageURL = split[1]
        }

        log.info "cytomineAuthenticationRequired : $cytomineAuthenticationRequired"

        /*log.info "URL " + params.url
        log.info "METHOD " + params.method
        log.info "contrast " + params.contrast
        log.info "brightness " + params.brightness*/

        try {
            /* Create Buffered Image  From URL */
            BufferedImage bufferedImage
            bufferedImage = getImageFromURL(imageURL)

            /* Process the BufferedImage */

            if (params.method == "r_rgb") {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                Colour_Deconvolution dt = new Colour_Deconvolution()
                dt.setSelectedStain(14)
                dt.setup(params.url, ip)
                dt.run(ip.getProcessor())
                bufferedImage = dt.getResult(0).getBufferedImage()
            }

            else if (params.method == "g_rgb") {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                Colour_Deconvolution dt = new Colour_Deconvolution()
                dt.setSelectedStain(14)
                dt.setup(params.url, ip)
                dt.run(ip.getProcessor())

                bufferedImage = dt.getResult(1).getBufferedImage()
            }

            else if (params.method == "b_rgb") {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                Colour_Deconvolution dt = new Colour_Deconvolution()
                dt.setSelectedStain(14)
                dt.setup(params.url, ip)
                dt.run(ip.getProcessor())
                bufferedImage = dt.getResult(2).getBufferedImage()
            }

            else if (params.method == "c_cmy") {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                Colour_Deconvolution dt = new Colour_Deconvolution()
                dt.setSelectedStain(15)
                dt.setup(params.url, ip)
                dt.run(ip.getProcessor())
                bufferedImage = dt.getResult(0).getBufferedImage()
            }
            else if (params.method == "m_cmy") {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                Colour_Deconvolution dt = new Colour_Deconvolution()
                dt.setSelectedStain(15)
                dt.setup(params.url, ip)
                dt.run(ip.getProcessor())
                bufferedImage = dt.getResult(1).getBufferedImage()
            }

            else if (params.method == "y_cmy") {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                Colour_Deconvolution dt = new Colour_Deconvolution()
                dt.setSelectedStain(15)
                dt.setup(params.url, ip)
                dt.run(ip.getProcessor())
                bufferedImage = dt.getResult(2).getBufferedImage()
            }

            else if (params.method == "he-eosin") {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                Colour_Deconvolution dt = new Colour_Deconvolution()
                dt.setSelectedStain(1)
                dt.setup(params.url, ip)
                dt.run(ip.getProcessor())
                bufferedImage = dt.getResult(1).getBufferedImage()
            }

            else if (params.method == "he-haematoxylin") {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                Colour_Deconvolution dt = new Colour_Deconvolution()
                dt.setSelectedStain(1)
                dt.setup(params.url, ip)
                dt.run(ip.getProcessor())
                bufferedImage = dt.getResult(0).getBufferedImage()
            }

            else if (params.method == "hdab-haematoxylin") {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                Colour_Deconvolution dt = new Colour_Deconvolution()
                dt.setSelectedStain(3)
                dt.setup(params.url, ip)
                dt.run(ip.getProcessor())
                bufferedImage = dt.getResult(0).getBufferedImage()
            }

            if (params.method == "hdab-dab") {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                Colour_Deconvolution dt = new Colour_Deconvolution()
                dt.setSelectedStain(3)
                dt.setup(params.url, ip)
                dt.run(ip.getProcessor())
                bufferedImage = dt.getResult(1).getBufferedImage()
            }

            else if (params.method == "binary") {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                ImageConverter ic = new ImageConverter(ip)
                ic.convertToGray8()
                ip.getProcessor().autoThreshold()
                bufferedImage = ip.getBufferedImage()
            }

            else if (params.method == "gray") {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                ImageConverter ic = new ImageConverter(ip)
                ic.convertToGray8()
                bufferedImage = ip.getBufferedImage()
            }

            else if (params.method == "otsu") {
                /*ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                ImageConverter ic = new ImageConverter(ip)
                ic.convertToGray8()
                Multi_OtsuThreshold dt = new Multi_OtsuThreshold()
                dt.setup(imageURL, ip)
                dt.run(ip.getProcessor())
                bufferedImage = dt.getResult().getBufferedImage()*/
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "Otsu")
            }
            /* Apply filters */
            else if (params.method == "huang") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "Huang")
            }
            else if (params.method == "intermodes") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "Intermodes")
            }
            else if (params.method == "isodata") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "IsoData")
            }
            else if (params.method == "li") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "Li")
            }
            else if (params.method == "maxentropy") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "MaxEntropy")
            }
            else if (params.method == "mean") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "Mean")
            }
            else if (params.method == "minerror") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "MinError(I)")
            }
            else if (params.method == "minimum") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "Minimum")
            }
            else if (params.method == "moments") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "Moments")
            }
            else if (params.method == "percentile") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "percentile")
            }
            else if (params.method == "renyientropy") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "RenyiEntropy")
            }
            else if (params.method == "shanbhag") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "Shanbhag")
            }
            else if (params.method == "triangle") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "Triangle")
            }
            else if (params.method == "yen") {
                bufferedImage = imageProcessingService.dynBinary(imageURL, bufferedImage, "Yen")
            }

            /* Apply filters */
            if (Boolean.parseBoolean(params.enhance)) {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                new ContrastEnhancer().stretchHistogram(ip, 0.5)
                bufferedImage = ip.getBufferedImage()
            }

            if (Boolean.parseBoolean(params.invert)) {
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                ip.getProcessor().invert()
                bufferedImage = ip.getBufferedImage()
            }

            if (params.brightness != null && params.contrast != null) {
                double brightness = Double.parseDouble(params.brightness)
                double contrast = Double.parseDouble(params.contrast)
                ImagePlus ip = new ImagePlus(imageURL, bufferedImage)
                double defaultMin = ip.getDisplayRangeMin()
                double defaultMax = ip.getDisplayRangeMax()
                double max = ip.getDisplayRangeMax()
                double min = ip.getDisplayRangeMin()
                double range = defaultMax - defaultMin
                int fullRange = 256

                //BRIGHTNESS
                def center = defaultMin + (defaultMax - defaultMin) * ((range - brightness) / range);
                double width = max - min;

                min = center - width / 2.0;
                max = center + width / 2.0;

                //CONTRAST
                center = min + (max - min) / 2.0;
                double mid = fullRange / 2
                double slope
                if (contrast <= mid) {
                    slope = brightness / mid
                }
                else {
                    slope = mid / (fullRange - contrast)
                }
                if (slope > 0.0) {
                    min = center - (0.5 * range) / slope
                    max = center + (0.5 * range) / slope;
                }

                //log.info("MIN/MAX : " + Math.round(min) + "/" + Math.round(max))
                ip.getProcessor().setMinAndMax(Math.round(min), Math.round(max))
                bufferedImage = ip.getBufferedImage()
            }

            /* Write response from BufferedImage */
            responseBufferedImage(bufferedImage)

        } catch (Exception e) {
            log.error e.toString()
            BufferedImage bufferedImage = getImageFromURL("/images/notavailable.jpg")
            responseBufferedImage(bufferedImage)
        }
    }


    def ij() {
        String tileURL = tileService.getTileUrl(params)
        BufferedImage tile = getImageFromURL(tileURL)
        BufferedImage newTile = null

        assert (tile)
        if (params.macro == "ec") {
            ImagePlus imp = new ImagePlus("", tile)
            //IJ.run(imp,"Enhance Contrast", "saturated=0.35");
            IJ.run(imp, "Make Binary", "")
            //IJ.run(imp, "Analyze Particles...", "size=50-Infinity circularity=0.00-1.00 show=Masks display record slice")
            /*IJ.run(imp,"Smooth", "")
            IJ.run(imp,"Find Edges", "")
            IJ.run(imp,"Add...", "value=25")
            IJ.run(imp,"Gaussian Blur...", "radius=2")
            IJ.run(imp,"Median...", "radius=1")
            IJ.run(imp,"Unsharp Mask...", "gaussian=2 mask=0.60")*/
            newTile = imp.getBufferedImage()
        }
        else {
            newTile = tile
        }
        responseBufferedImage(newTile)
    }

    /**
     * Extract all args into a list.
     * argStart0=val0&argStart1=val1&argStart2=val2... => [val0,val1, val2...]
     * @param argStart
     */
    private def extractParams(String argStart) {
        def list = []
        int i = 0;
        String nextUrlParams = params.get(argStart + i)
        while (nextUrlParams != null) {
            list << nextUrlParams
            i++
            nextUrlParams = params.get(argStart + i)
        }
        return list
    }
}
