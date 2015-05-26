package be.cytomine.image

import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.ImageFormat

/*
 * Copyright (c) 2009-2015. Authors: see NOTICE file.
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
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.annotation.RestApiResponseObject
import org.restapidoc.pojo.RestApiParamType
import utils.ImageUtils

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage

@RestApi(name = "image services", description = "Methods for images (thumb, tile, property, ...)")
class ImageController extends ImageUtilsController {

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
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
        BufferedImage bufferedImage = imageFormat.thumb(maxSize)
        bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
        if (bufferedImage) {
            responseBufferedImage(bufferedImage)
        } else {
            //return 404 image
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
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
        println "imageFormat=${imageFormat.class}"
        BufferedImage bufferedImage = imageFormat.associated(label)
        bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
        if (bufferedImage) {
            responseBufferedImage(bufferedImage)
        } else {
            //return 404 image
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
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
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
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
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

        if(params.location) {
            geometry = new WKTReader().read(params.location)
        } else if(request.JSON.location!=null && request.JSON.location!="") {
            geometry = new WKTReader().read(request.JSON.location)
        }

        bufferedImage = imageProcessingService.createMask(bufferedImage, geometry, params, params.boolean('alphaMask',false))
        //resize if necessary
        if (params.maxSize) {
            int maxSize = params.int('maxSize', 256)
            bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
        } else if (params.zoom) {
            int zoom = params.int('zoom', 0)
            int maxWidth = params.double('width') / Math.pow(2, zoom)
            int maxHeight = params.double('height') / Math.pow(2, zoom)
            bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxWidth, maxHeight)
        }
        responseBufferedImage(bufferedImage)
    }


    @RestApiMethod(description="Get the crop of an image", extensions = ["jpg","png"])
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
    ])
    def crop() {

        def savedWidth = params.double('width')
        def savedHeight = params.double('height')

        BufferedImage bufferedImage = readCropBufferedImage(params)


       if (params.draw) {
            String location = params.location
            Geometry geometry = new WKTReader().read(location)
            bufferedImage = imageProcessingService.createCropWithDraw(bufferedImage, geometry, params)
        } else if (params.mask) {
            String location = params.location
            Geometry geometry  = new WKTReader().read(location)
            bufferedImage = imageProcessingService.createMask(bufferedImage, geometry, params, false)
        } else if (params.alphaMask) {
           /*
           http://localhost:9080/image/crop.png?fif=%2Fdata%2Fbeta.cytomine.be%2F156037249%2F%2F1430310481926%2FAGDC1_PBS_6_-_2015-03-18_10.53.53.ndpi&mimeType=openslide/ndpi&topLeftX=7444&topLeftY=10286&width=240&height=232&imageWidth=23040&imageHeight=16896&alphaMask=true&location=POLYGON+%28%287484+10246%2C+7476+10238%2C+7460+10238%2C+7460+10230%2C+7452+10230%2C+7444+10230%2C+7444+10222%2C+7444+10214%2C+7444+10206%2C+7444+10198%2C+7444+10190%2C+7444+10182%2C+7444+10174%2C+7444+10166%2C+7452+10158%2C+7452+10150%2C+7452+10142%2C+7452+10134%2C+7460+10118%2C+7468+10110%2C+7476+10102%2C+7476+10094%2C+7484+10086%2C+7492+10086%2C+7492+10078%2C+7500+10070%2C+7508+10070%2C+7516+10070%2C+7516+10062%2C+7524+10062%2C+7532+10062%2C+7532+10054%2C+7540+10054%2C+7548+10054%2C+7556+10054%2C+7564+10054%2C+7572+10054%2C+7580+10054%2C+7588+10054%2C+7596+10054%2C+7604+10054%2C+7612+10054%2C+7620+10054%2C+7628+10054%2C+7636+10054%2C+7644+10054%2C+7652+10062%2C+7652+10070%2C+7660+10070%2C+7660+10078%2C+7660+10086%2C+7668+10094%2C+7668+10102%2C+7676+10110%2C+7676+10118%2C+7676+10126%2C+7676+10134%2C+7684+10142%2C+7684+10150%2C+7684+10158%2C+7684+10166%2C+7684+10174%2C+7684+10182%2C+7684+10190%2C+7684+10198%2C+7676+10206%2C+7676+10214%2C+7668+10222%2C+7668+10230%2C+7660+10238%2C+7652+10246%2C+7644+10254%2C+7636+10262%2C+7628+10262%2C+7620+10270%2C+7612+10278%2C+7604+10286%2C+7596+10286%2C+7588+10286%2C+7580+10286%2C+7572+10286%2C+7564+10286%2C+7556+10286%2C+7548+10286%2C+7540+10286%2C+7532+10286%2C+7524+10278%2C+7484+10246%29%29&resolution=0.4567461311817169


                http://localhost:9080/image/crop.png?fif=%2Fdata%2Fbeta.cytomine.be%2F156037249%2F%2F1430310481926%2FAGDC1_PBS_6_-_2015-03-18_10.53.53.ndpi&mimeType=openslide/ndpi&topLeftX=7444&topLeftY=10286&width=240&height=232&imageWidth=23040&imageHeight=16896&zoom=0&alphaMask=true&location=POLYGON+%28%287484+10246%2C+7476+10238%2C+7460+10238%2C+7460+10230%2C+7452+10230%2C+7444+10230%2C+7444+10222%2C+7444+10214%2C+7444+10206%2C+7444+10198%2C+7444+10190%2C+7444+10182%2C+7444+10174%2C+7444+10166%2C+7452+10158%2C+7452+10150%2C+7452+10142%2C+7452+10134%2C+7460+10118%2C+7468+10110%2C+7476+10102%2C+7476+10094%2C+7484+10086%2C+7492+10086%2C+7492+10078%2C+7500+10070%2C+7508+10070%2C+7516+10070%2C+7516+10062%2C+7524+10062%2C+7532+10062%2C+7532+10054%2C+7540+10054%2C+7548+10054%2C+7556+10054%2C+7564+10054%2C+7572+10054%2C+7580+10054%2C+7588+10054%2C+7596+10054%2C+7604+10054%2C+7612+10054%2C+7620+10054%2C+7628+10054%2C+7636+10054%2C+7644+10054%2C+7652+10062%2C+7652+10070%2C+7660+10070%2C+7660+10078%2C+7660+10086%2C+7668+10094%2C+7668+10102%2C+7676+10110%2C+7676+10118%2C+7676+10126%2C+7676+10134%2C+7684+10142%2C+7684+10150%2C+7684+10158%2C+7684+10166%2C+7684+10174%2C+7684+10182%2C+7684+10190%2C+7684+10198%2C+7676+10206%2C+7676+10214%2C+7668+10222%2C+7668+10230%2C+7660+10238%2C+7652+10246%2C+7644+10254%2C+7636+10262%2C+7628+10262%2C+7620+10270%2C+7612+10278%2C+7604+10286%2C+7596+10286%2C+7588+10286%2C+7580+10286%2C+7572+10286%2C+7564+10286%2C+7556+10286%2C+7548+10286%2C+7540+10286%2C+7532+10286%2C+7524+10278%2C+7484+10246%29%29&resolution=0.4567461311817169


           should be white (transparent!!!!)
            change scale image to be transparent


            */




            String location = params.location
            Geometry geometry = new WKTReader().read(location)
            bufferedImage = imageProcessingService.createMask(bufferedImage, geometry, params, true)
        }
        //resize if necessary
        if (params.maxSize) {
            int maxSize = params.int('maxSize', 256)
            bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
        } else if (params.zoom) {
            int zoom = params.int('zoom', 0)
            int maxWidth = savedWidth / Math.pow(2, zoom)
            int maxHeight = savedHeight / Math.pow(2, zoom)
            bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxWidth, maxHeight)
        }
		
        if(params.boolean('drawScaleBar')) {
            double proport1 = params.double('width')/params.double('height')
            double porpert2 = (double)bufferedImage.getWidth()/(double)bufferedImage.getHeight()
//            if(proport1==porpert2) {
                //If the crop mage has been resized, the image may be "cut" (how to know that?).
                //(we may have oldWidth/oldHeight <> newWidth/newHeight)
                //This mean that its impossible to compute the real size of the image because the size of the image change (not a problem) AND the image change (the image server cut somepart of the image).
                //I first try to compute the ratio (double ratioWidth = (double)((double)bufferedImage.getWidth()/params.double('width'))),
                //but if the image is cut , its not possible to compute the good width size
                double ratioWidth = (double)((double)bufferedImage.getWidth()/params.double('width'))
                Double resolution = params.double('resolution')
                Double magnification = params.double('magnification')					
                bufferedImage = imageProcessingService.drawScaleBar(bufferedImage, resolution,ratioWidth, magnification)
//            }
        } 
        responseBufferedImage(bufferedImage)
    }

    public BufferedImage readCropBufferedImage(def params) {
        String fif = params.fif
        String mimeType = params.mimeType
        def savedTopX = params.topLeftX
        def savedTopY = params.topLeftY

        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)

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
        BufferedImage bufferedImage = ImageIO.read(new URL(cropURL))

        Long start = System.currentTimeMillis()

        /*
         * When we ask a crop with size = w*h, we translate w to 1d/(imageWidth / width) for IIP server request. Same for h.
         * We may loose precision and the size could be w+-1 * h+-1.
         * If the difference is < as threshold, we rescale
         */
        int threshold = 10
        boolean imageDifferentSize = (savedWidth != bufferedImage.width) || (savedHeight != bufferedImage.height)
        if (imageDifferentSize && (Math.abs(savedWidth - bufferedImage.width) < threshold && Math.abs(savedHeight - bufferedImage.height) < threshold)) {
            bufferedImage = ImageUtils.resize(bufferedImage, (int) savedWidth, (int) savedHeight)
        }

        println "time=${System.currentTimeMillis() - start}"

        int i = 0
        while (bufferedImage == null && i < 3) {
            bufferedImage = ImageIO.read(new URL(cropURL))
            i++
        }

        if (bufferedImage == null) {
            throw new Exception("Not a valid image: ${cropURL}")
        }

        params.topLeftX = savedTopX
        params.topLeftY = savedTopY
        params.width = savedWidth
        params.height = savedHeight

        if (params.safe) {
            //if safe mode, skip annotation too large
            if (params.int('width') > grailsApplication.config.cytomine.maxAnnotationOnImageWidth) throw new Exception("Too big annotation!")
            if (params.int('height') > grailsApplication.config.cytomine.maxAnnotationOnImageWidth) throw new Exception("Too big annotation!")
        }
        bufferedImage
    }

    @RestApiMethod(description="Get a tile of an image (following zoomify format)", extensions = ["jpg","png"])
    @RestApiParams(params=[
    @RestApiParam(name="zoomify", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
    @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
    @RestApiParam(name="tileGroup", type="int", paramType = RestApiParamType.QUERY, description = "The Tile Group (see zoomify format)"),
    @RestApiParam(name="z", type="int", paramType = RestApiParamType.QUERY, description = "The Z index (see zoomify format)"),
    @RestApiParam(name="x", type="int", paramType = RestApiParamType.QUERY, description = "The X index (see zoomify format)"),
    @RestApiParam(name="y", type="int", paramType = RestApiParamType.QUERY, description = "The Y index (see zoomify format)")
    ])
    def tile() {
        responseImageFromUrl(tileService.getTileUrl(params))
        //redirect(url:tileService.getTileUrl(params))
    }

    @RestApiMethod(description="Download an image", extensions = ["jpg","png"])
    @RestApiParams(params=[
    @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
    ])
    def download() {
        String fif = params.get("fif")
        responseFile(new File(fif))
    }


}
