package be.cytomine.image

import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.ImageFormat
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON
import ij.ImagePlus
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

@RestApi(name = "image services", description = "Methods for images (thumb, tile, property, ...)")
class ImageController extends ImageUtilsController {

    def imageProcessingService

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
        String fif = params.fif
        String mimeType = params.mimeType
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)

//        def savedTopX = params.topLeftX
//        def savedTopY = params.topLeftY
//        def savedWidth = params.double('width')
//        def savedHeight = params.double('height')
//
//        println "**********************"
//        println params.increaseArea
//
//        if(params.double('increaseArea')) {
//            params.width = params.int('width')*params.double("increaseArea")
//            params.height =   params.int('height')*params.double("increaseArea")
//            params.topLeftX = params.int('topLeftX')-((params.double('width')-savedWidth)/2)
//            params.topLeftY = params.int('topLeftY')+((params.double('height')-savedHeight)/2)
//        }

        String cropURL = imageFormat.cropURL(params)
        BufferedImage bufferedImage = ImageIO.read(new URL(cropURL))

//        params.topLeftX = savedTopX
//        params.topLeftY = savedTopY
//        params.width = savedWidth
//        params.height = savedHeight
//
        println "drawScaleBar=${params.boolean('drawScaleBar')}"
        if(params.boolean('drawScaleBar')) {
            Double resolution = params.double('resolution')
            bufferedImage = imageProcessingService.drawScaleBar(bufferedImage, resolution)
        } else if (params.draw) {
            String location = params.location
            Geometry geometry = new WKTReader().read(location)
            bufferedImage = imageProcessingService.createCropWithDraw(bufferedImage, geometry, params)
        } else if (params.mask) {
            String location = params.location
            Geometry geometry  = new WKTReader().read(location)
            bufferedImage = imageProcessingService.createMask(bufferedImage, geometry, params, false)
            new ImagePlus("", bufferedImage).show()
        } else if (params.alphaMask) {
            String location = params.location
            Geometry geometry = new WKTReader().read(location)
            bufferedImage = imageProcessingService.createMask(bufferedImage, geometry, params, true)
            new ImagePlus("", bufferedImage).show()
        }
        //resize if necessary
        if (params.maxSize) {
            int maxSize = params.int('maxSize', 256)
            bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
        } else if (params.zoom) {
            int zoom = params.int('zoom', 0)
            int maxWidth = bufferedImage.width / Math.pow(2, zoom)
            int maxHeight = bufferedImage.width / Math.pow(2, zoom)
            bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxWidth, maxHeight)
        }
        responseBufferedImage(bufferedImage)
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
        String fif = params.zoomify
        /*remove the "/" at the end of the path injected by openlayers (OL2).
          I Did not find the way to avoid it from OL2 (BS)
         */
        fif = fif.substring(0, fif.length()-1)
        String mimeType = params.mimeType
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
        //todo, use mimetype to have imageFormat identification
        String tileURL = imageFormat.tileURL(fif, params)
        responseImageFromUrl(tileURL)

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
