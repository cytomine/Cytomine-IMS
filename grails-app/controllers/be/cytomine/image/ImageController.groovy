package be.cytomine.image

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

import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.supported.SupportedImageFormat
import be.cytomine.exception.MiddlewareException
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON
import grails.util.Holders
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.annotation.RestApiResponseObject
import org.restapidoc.pojo.RestApiParamType
import utils.ImageUtils
import utils.ServerUtils

@RestApi(name = "image services", description = "Methods for images (thumb, tile, property, ...)")
class ImageController extends ImageResponseController {

    def imageProcessingService
    def tileService

    @RestApiMethod(description="Get the thumb of an image", extensions = ["jpg","png"])
    @RestApiParams(params=[
    @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
    @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
    @RestApiParam(name="maxSize", type="int", paramType = RestApiParamType.QUERY, description = "The max width or height of the generated thumb", required = false)
    ])
    def thumb() {
        String fif = params.fif
        int maxSize = params.int('maxSize', 512)
        String mimeType = params.mimeType
        SupportedImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
        BufferedImage bufferedImage = imageFormat.thumb(maxSize)
        bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
        if (bufferedImage) {
            withFormat {
                png { responseBufferedImagePNG(bufferedImage) }
                jpg { responseBufferedImageJPG(bufferedImage) }
            }
        }
    }

    @RestApiMethod(description="Get a nested (or associated) image (e.g. macro) of an image", extensions = ["jpg","png"])
    @RestApiParams(params=[
    @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
    @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
    @RestApiParam(name="label", type="String", paramType = RestApiParamType.QUERY, description = "The requested nested image, identified by label (e.g. macro)"),
    @RestApiParam(name="maxSize", type="int", paramType = RestApiParamType.QUERY, description = " The max width or height of the generated thumb", required = false)
    ])
    def nested() {
        String fif = params.fif
        String label = params.label
        String mimeType = params.mimeType
        int maxSize = params.int('maxSize', 512)
        SupportedImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
        log.info "imageFormat=${imageFormat.class}"
        BufferedImage bufferedImage = imageFormat.associated(label)
        bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
        if (bufferedImage) {
            withFormat {
                png { responseBufferedImagePNG(bufferedImage) }
                jpg { responseBufferedImageJPG(bufferedImage) }
            }
        }
    }

    @RestApiMethod(description="Get the list of nested (or associated) images available of an image", extensions = ["json"])
    @RestApiParams(params=[
    @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
    @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
    ])
    def associated() {
        String fif = params.fif
        String mimeType = params.mimeType
        SupportedImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
        render imageFormat.associated() as JSON
    }

    @RestApiMethod(description="Get the available properties (with, height, resolution, magnitude, ...) of an image", extensions = ["json"])
    @RestApiParams(params=[
    @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
    @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
    ])
    def properties() {
        String fif = params.fif
        String mimeType = params.mimeType
        SupportedImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
        render imageFormat.properties() as JSON
    }

    @RestApiMethod(description="Get the mask of a crop image", extensions = ["jpg","png"])
    @RestApiResponseObject(objectIdentifier =  "[location : wkt]")
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
            @RestApiParam(name="topLeftX", type="int", paramType = RestApiParamType.QUERY, description = "The top left X value of the requested ROI"),
            @RestApiParam(name="topLeftX", type="int", paramType = RestApiParamType.QUERY, description = "The top left Y value of the requested ROI"),
            @RestApiParam(name="width", type="int", paramType = RestApiParamType.QUERY, description = "The width of the ROI (in pixels)"),
            @RestApiParam(name="height", type="int", paramType = RestApiParamType.QUERY, description = "The height of the ROI (in pixels)"),
            @RestApiParam(name="imageWidth", type="int", paramType = RestApiParamType.QUERY, description = "The image width of the whole image"),
            @RestApiParam(name="imageHeight", type="int", paramType = RestApiParamType.QUERY, description = "The image height of the whole image"),
            @RestApiParam(name="maxSize", type="int", paramType = RestApiParamType.QUERY, description = " The max width or height of the generated thumb", required = false),
            @RestApiParam(name="zoom", type="int", paramType = RestApiParamType.QUERY, description = " The zoom used in order to extract the ROI (0 = higher resolution). Ignored if maxSize is used.", required = false),
            @RestApiParam(name="alphaMask", type="boolean", paramType = RestApiParamType.QUERY, description = " If used, return the crop with the mask in the alphachannel (0% to 100%). PNG required", required = false),
    ])
    def mask() {
        BufferedImage bufferedImage = readCropBufferedImage(params)

        def geometry = null
        if (params.location) {
            geometry = new WKTReader().read(params.location)
        }
        else if (request.JSON.location != null && request.JSON.location != "") {
            geometry = new WKTReader().read(request.JSON.location)
        }

        bufferedImage = imageProcessingService.createMask(bufferedImage, geometry, params, params.boolean('alphaMask', false))
        //resize if necessary
        if (params.maxSize) {
            int maxSize = params.int('maxSize', 256)
            bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
        }
        else if (params.zoom) {
            int zoom = params.int('zoom', 0)
            int maxWidth = params.double('width') / Math.pow(2, zoom)
            int maxHeight = params.double('height') / Math.pow(2, zoom)
            bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxWidth, maxHeight)
        }

        withFormat {
            png { responseBufferedImagePNG(bufferedImage) }
            jpg { responseBufferedImageJPG(bufferedImage) }
        }
    }


    @RestApiMethod(description="Get the crop of an image", extensions = ["jpg", "png", "tiff"])
    @RestApiParams(params=[
    @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
    @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
    @RestApiParam(name="topLeftX", type="int", paramType = RestApiParamType.QUERY, description = "The top left X value of the requested ROI"),
    @RestApiParam(name="topLeftX", type="int", paramType = RestApiParamType.QUERY, description = "The top left Y value of the requested ROI"),
    @RestApiParam(name="width", type="int", paramType = RestApiParamType.QUERY, description = "The width of the ROI (in pixels)"),
    @RestApiParam(name="height", type="int", paramType = RestApiParamType.QUERY, description = "The height of the ROI (in pixels)"),
    @RestApiParam(name="imageWidth", type="int", paramType = RestApiParamType.QUERY, description = "The image width of the whole image"),
    @RestApiParam(name="imageHeight", type="int", paramType = RestApiParamType.QUERY, description = "The image height of the whole image"),
    @RestApiParam(name="maxSize", type="int", paramType = RestApiParamType.QUERY, description = " The max width or height of the generated thumb", required = false),
    @RestApiParam(name="zoom", type="int", paramType = RestApiParamType.QUERY, description = " The zoom used in order to extract the ROI (0 = higher resolution). Ignored if maxSize is used.", required = false),
    @RestApiParam(name="location", type="int", paramType = RestApiParamType.QUERY, description = " A geometry in WKT Format (Well-known text)", required = false),
    @RestApiParam(name="draw", type="int", paramType = RestApiParamType.QUERY, description = " If used, draw the geometry contour on the crop. draw takes precedence over mask & alphamask.", required = false),
    @RestApiParam(name="mask", type="int", paramType = RestApiParamType.QUERY, description = " If used, return the mask of the geometry (black & white) instead of the crop. mask takes precedence over alphamask", required = false),
    @RestApiParam(name="alphaMask", type="int", paramType = RestApiParamType.QUERY, description = " If used, return the crop with the mask in the alphachannel (0% to 100%). PNG required", required = false),
    @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file (see IIP format)"),
    @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed (see IIP format)"),
    @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast (see IIP format)"),
    @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction (see IIP format)")
    ])
    def crop() {
        def savedWidth = params.double('width')
        def savedHeight = params.double('height')

        log.info params

        SupportedImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(params.fif, params.mimeType)

        boolean exactSize = ServerUtils.getServers(Holders.config.cytomine.iipImageServerCyto).containsAll(imageFormat.iipURL)

        BufferedImage bufferedImage = readCropBufferedImage(params)

        if (params.boolean("point")) {
            imageProcessingService.drawPoint(bufferedImage)
        }

        if (params.draw) {
            String location = params.location
            Geometry geometry = new WKTReader().read(location)
            bufferedImage = imageProcessingService.createCropWithDraw(bufferedImage, geometry, params)
        }
        else if (params.mask) {
            String location = params.location
            Geometry geometry = new WKTReader().read(location)
            bufferedImage = imageProcessingService.createMask(bufferedImage, geometry, params, false)
        }
        else if (params.alphaMask) {
            String location = params.location
            Geometry geometry = new WKTReader().read(location)

            if (params.zoom) {
                int zoom = params.int('zoom', 0)
                int maxWidth = savedWidth / Math.pow(2, zoom)
                int maxHeight = savedHeight / Math.pow(2, zoom)
                //resize and preserve png transparency for alpha mask
                //bufferedImage = Scalr.resize(bufferedImage,  Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH, maxWidth,
                // maxHeight, Scalr.OP_ANTIALIAS);
                bufferedImage = imageProcessingService.resizeImage(bufferedImage, maxWidth, maxHeight)
            }

            bufferedImage = imageProcessingService.createMask(bufferedImage, geometry, params, true)
        }

        //resize if necessary
        if (params.maxSize) {
            //useless with new iipversion
            if (!exactSize) {
                int maxSize = params.int('maxSize', 256)
                bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
            }
        }
        else if (params.zoom && !params.alphaMask) {
            int zoom = params.int('zoom', 0)
            int maxWidth = savedWidth / Math.pow(2, zoom)
            int maxHeight = savedHeight / Math.pow(2, zoom)
            bufferedImage = imageProcessingService.resizeImage(bufferedImage, maxWidth, maxHeight)
        }

        if (params.boolean('drawScaleBar')) {
            //If the crop mage has been resized, the image may be "cut" (how to know that?).
            //(we may have oldWidth/oldHeight <> newWidth/newHeight)
            //This mean that its impossible to compute the real size of the image because the size of the image change
            // (not a problem) AND the image change (the image server cut somepart of the image).
            //I first try to compute the ratio (double ratioWidth = (double)((double)bufferedImage.getWidth()/params.double('width'))),
            //but if the image is cut , its not possible to compute the good width size
            double ratioWidth = (double) ((double) bufferedImage.getWidth() / params.double('width'))
            Double resolution = params.double('resolution')
            Double magnification = params.double('magnification')
            bufferedImage = imageProcessingService.drawScaleBar(bufferedImage, resolution, ratioWidth, magnification)
        }

        withFormat {
            png { responseBufferedImagePNG(bufferedImage) }
            jpg { responseBufferedImageJPG(bufferedImage) }
            tiff { responseBufferedImageTIFF(bufferedImage) }
        }

    }

    @RestApiMethod(description="Get a tile of an image (following zoomify format)", extensions = ["jpg"])
    @RestApiParams(params=[
    @RestApiParam(name="zoomify", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
    @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
    @RestApiParam(name="tileGroup", type="int", paramType = RestApiParamType.QUERY, description = "The Tile Group (see zoomify format)"),
    @RestApiParam(name="z", type="int", paramType = RestApiParamType.QUERY, description = "The Z index (see zoomify format)"),
    @RestApiParam(name="x", type="int", paramType = RestApiParamType.QUERY, description = "The X index (see zoomify format)"),
    @RestApiParam(name="y", type="int", paramType = RestApiParamType.QUERY, description = "The Y index (see zoomify format)")
    ])
    def tileZoomify() {
        responseJPGImageFromUrl(tileService.getTileUrlZoomify(params))
    }

    @RestApiMethod(description="Get a tile of an image (following IIP format)", extensions = ["jpg"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
            @RestApiParam(name="tileIndex", type="int", paramType = RestApiParamType.QUERY, description = "The Tile Index (see IIP format)"),
            @RestApiParam(name="z", type="int", paramType = RestApiParamType.QUERY, description = "The Z index (see IIP format)"),
            @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file (see IIP format)"),
            @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed (see IIP format)"),
            @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast (see IIP format)"),
            @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction (see IIP format)")
    ])
    def tileIIP() {
        responseJPGImageFromUrl(tileService.getTileUrlIIP(params))
    }

    @RestApiMethod(description="Download an image")
    @RestApiParams(params=[
    @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
    ])
    def download() {
        String fif = params.get("fif")
        responseFile(new File(fif))
    }


    BufferedImage readCropBufferedImage(def params) {
        String fif = params.fif
        String mimeType = params.mimeType
        def savedTopX = params.topLeftX
        def savedTopY = params.topLeftY

        SupportedImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)

        def savedWidth = params.double('width')
        def savedHeight = params.double('height')

        if (params.double('increaseArea')) {
            params.width = params.int('width') * params.double("increaseArea")
            params.height = params.int('height') * params.double("increaseArea")
            params.topLeftX = params.int('topLeftX') - ((params.double('width') - savedWidth) / 2)
            params.topLeftY = params.int('topLeftY') + ((params.double('height') - savedHeight) / 2)
        }

        String cropURL = imageFormat.cropURL(params, grailsApplication.config.cytomine.charset)
        log.info cropURL

        boolean exactSize = ServerUtils.getServers(Holders.config.cytomine.iipImageServerCyto).containsAll(imageFormat.iipURL)

        for (String format : ImageIO.getWriterFormatNames()) {
            System.out.println("format = " + format);
        }

        BufferedImage bufferedImage = ImageIO.read(new URL(cropURL))
        int i = 0
        while (bufferedImage == null && i < 3) {
            bufferedImage = ImageIO.read(new URL(cropURL))
            i++
        }

        if (bufferedImage == null) {
            throw new MiddlewareException("Not a valid image: ${cropURL}")
        }

        Long start = System.currentTimeMillis()

        /*
         * When we ask a crop with size = w*h, we translate w to 1d/(imageWidth / width) for old IIP server request. Same for h.
         * We may loose precision and the size could be w+-1 * h+-1.
         * If the difference is < as threshold, we rescale
         */
        if (!exactSize) {
            int threshold = 10
            boolean imageDifferentSize = (savedWidth != bufferedImage.width) || (savedHeight != bufferedImage.height)
            // TODO so if increase area is set, it is possible than we have no effect :s ==> fix with increaseArea
            if (imageDifferentSize
                    && (Math.abs(savedWidth - bufferedImage.width) < threshold
                        && Math.abs(savedHeight - bufferedImage.height) < threshold)) {
                bufferedImage = ImageUtils.resize(bufferedImage, (int) savedWidth, (int) savedHeight)
            }
        }

        log.info "time=${System.currentTimeMillis() - start}"

        params.topLeftX = savedTopX
        params.topLeftY = savedTopY
        params.width = savedWidth
        params.height = savedHeight

        if (params.safe) {
            //if safe mode, skip annotation too large
            if ((params.int('width') > grailsApplication.config.cytomine.maxAnnotationOnImageWidth) ||
                    (params.int('height') > grailsApplication.config.cytomine.maxAnnotationOnImageWidth)) {
                throw new MiddlewareException("Too big annotation!")
            }
        }

        bufferedImage
    }
}
