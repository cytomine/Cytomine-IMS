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

@RestApi(name = "images services", description = "Methods for requests images")
class ImageController extends ImageUtilsController {

    def imageProcessingService

    @RestApiMethod(description="Get the thumb of an image")
    @RestApiParams(params=[
    @RestApiParam(name="fif", type="string", paramType = RestApiParamType.QUERY, description = "The absolute path of the full image"),
    @RestApiParam(name="maxSize", type="long", paramType = RestApiParamType.QUERY, description = "The maximum widh or height of the thumb")
    ])
    def thumb() {
        String fif = params.fif
        int maxSize = params.int('maxSize', 256)
        String mimeType = params.mimeType
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(mimeType)
        BufferedImage bufferedImage = imageFormat.thumb(maxSize)
        bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
        if (bufferedImage) {
            responseBufferedImage(bufferedImage)
        } else {
            //return 404 image
        }
    }

    def nested() {
        String fif = params.fif
        String label = params.label
        String mimeType = params.mimeType
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(mimeType)
        BufferedImage bufferedImage = imageFormat.associated(label)
        if (bufferedImage) {
            responseBufferedImage(bufferedImage)
        } else {
            //return 404 image
        }

    }

    def associated() {
        String fif = params.fif
        String mimeType = params.mimeType
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(mimeType)
        render imageFormat.associated() as JSON
    }

    def properties() {
        String fif = params.fif
        String mimeType = params.mimeType
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(mimeType)
        render imageFormat.properties() as JSON
    }

    def crop() {
        String fif = params.fif
        String mimeType = params.mimeType
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(mimeType)
        String cropURL = imageFormat.cropURL(params)
        BufferedImage bufferedImage = ImageIO.read(new URL(cropURL))

        if (params.draw) {
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

    def tile() {
        String fif = params.zoomify
        /*remove the "/" at the end of the path injected by openlayers (OL2).
          I Did not find the way to avoid it from OL2 (BS)
         */
        fif = fif.substring(0, fif.length()-1)
        String mimeType = params.mimeType
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(mimeType)
        //todo, use mimetype to have imageFormat identification
        String tileURL = imageFormat.tileURL(fif, params)
        responseImageFromUrl(tileURL)

    }

    def download() {
        String fif = params.get("fif")
        responseFile(new File(fif))
    }


}
