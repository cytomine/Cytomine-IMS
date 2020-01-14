package be.cytomine.image

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

import be.cytomine.exception.MiddlewareException
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.supported.NativeFormat
import be.cytomine.formats.tools.CytomineFile
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

@RestApi(name = "slice services", description = "Methods for slices (thumb, crops, ...)")
class SliceController extends ImageResponseController {

    def imageProcessingService
    def histogramService

    @RestApiMethod(description = "Get the histogram and statistics about a slice", extensions = ["json"])
    @RestApiParams(params = [
            @RestApiParam(name = "fif", type = "String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name = "mimeType", type = "String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
            @RestApiParam(name = "bitPerSample", type = "int", paramType = RestApiParamType.QUERY, description = "The number of bits per sample of the image"),
            @RestApiParam(name = "samplePerPixel", type = "int", paramType = RestApiParamType.QUERY, description = "The number of sample per pixel of the image"),
    ])
    def histogram() {
        String fif = URLDecoder.decode(params.fif, "UTF-8")
        String mimeType = params.mimeType
        NativeFormat imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType, true)

        int bps = params.int("bitPerSample", 8)
        int spp = params.int("samplePerPixel", 1)

        def data = []
        for (int i = 0; i < spp; i++) {
            def histogram = imageFormat.histogram(i)

            if (histogram.isEmpty())
                break;

            data << [
                    sample: i,
                    min: histogramService.min(histogram),
                    max: histogramService.max(histogram),
                    histogram: histogram,
                    histogram256: histogramService.binnedHistogram(histogram, 256, bps)
            ]
        }

        render data as JSON
    }

    @RestApiMethod(description="Get the thumb of a slice", extensions = ["jpg","png", "tiff"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
            @RestApiParam(name="maxSize", type="int", paramType = RestApiParamType.QUERY, description = "The max width or height of the generated thumb", required = false),
            @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file (see IIP format)", required = false),
            @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed (see IIP format)", required = false),
            @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast (see IIP format)", required = false),
            @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction (see IIP format)", required = false),
            @RestApiParam(name="bits", type="int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel (see IIP format)", required = false)
    ])
    def thumb() {
        String fif = URLDecoder.decode(params.fif,"UTF-8")
        int maxSize = params.int('maxSize', 512)
        params.maxSize = maxSize
        String mimeType = params.mimeType
        NativeFormat imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType, true)
        BufferedImage bufferedImage = imageFormat.thumb(params)
        if (bufferedImage) {
            bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
            withFormat {
                png { responseBufferedImagePNG(bufferedImage) }
                jpg { responseBufferedImageJPG(bufferedImage) }
                tiff { responseBufferedImageTIFF(bufferedImage) }
            }
        }
    }

    @RestApiMethod(description="Get the crop of a slice", extensions = ["jpg", "png", "tiff"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
            @RestApiParam(name="topLeftX", type="int", paramType = RestApiParamType.QUERY, description = "The top left X value of the requested ROI"),
            @RestApiParam(name="topLeftY", type="int", paramType = RestApiParamType.QUERY, description = "The top left Y value of the requested ROI"),
            @RestApiParam(name="width", type="int", paramType = RestApiParamType.QUERY, description = "The width of the ROI (in pixels)"),
            @RestApiParam(name="height", type="int", paramType = RestApiParamType.QUERY, description = "The height of the ROI (in pixels)"),
            @RestApiParam(name="imageWidth", type="int", paramType = RestApiParamType.QUERY, description = "The image width of the whole image"),
            @RestApiParam(name="imageHeight", type="int", paramType = RestApiParamType.QUERY, description = "The image height of the whole image"),
            @RestApiParam(name="maxSize", type="int", paramType = RestApiParamType.QUERY, description = " The max width or height of the generated thumb", required = false),
            @RestApiParam(name="zoom", type="int", paramType = RestApiParamType.QUERY, description = " The zoom used in order to extract the ROI (0 = higher resolution). Ignored if maxSize is used.", required = false),
            @RestApiParam(name="location", type="int", paramType = RestApiParamType.QUERY, description = " A geometry in WKT Format (Well-known text)", required = false),
            @RestApiParam(name="draw", type="boolean", paramType = RestApiParamType.QUERY, description = " If used, draw the geometry contour on the crop. draw takes precedence over mask & alphamask.", required = false),
            @RestApiParam(name="mask", type="int", paramType = RestApiParamType.QUERY, description = " If used, return the mask of the geometry (black & white) instead of the crop. mask takes precedence over alphamask", required = false),
            @RestApiParam(name="thickness", type="int", paramType = RestApiParamType.QUERY, description = " If draw used, set the thickness of the geometry contour on the crop.", required = false),
            @RestApiParam(name="color", type="String", paramType = RestApiParamType.QUERY, description = " If draw used, set the color of the geometry contour on the crop.", required = false),
            @RestApiParam(name="square", type="boolean", paramType = RestApiParamType.QUERY, description = " If used, try to extends the ROI around the crop to have a square.", required = false),
            @RestApiParam(name="alphaMask", type="int", paramType = RestApiParamType.QUERY, description = " If used, return the crop with the mask in the alphachannel (0% to 100%). PNG required", required = false),
            @RestApiParam(name="drawScaleBar", type="int", paramType = RestApiParamType.QUERY, description = "If true, draw a scale bar", required = false),
            @RestApiParam(name="resolution", type="float", paramType = RestApiParamType.QUERY, description = "Resolution to print in scale bar if used", required=false),
            @RestApiParam(name="magnification", type="float", paramType = RestApiParamType.QUERY, description = "Magnification to print in scale bar if used", required=false),
            @RestApiParam(name="increaseArea", type="boolean",  paramType = RestApiParamType.QUERY, description = "", required=false),
            @RestApiParam(name="safe", type="boolean", paramType = RestApiParamType.QUERY, description = "If true, skip too large ROI (true by default)", required=false),
            @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file (see IIP format)", required = false),
            @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed (see IIP format)", required = false),
            @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast (see IIP format)", required = false),
            @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction (see IIP format)", required = false),
            @RestApiParam(name="bits", type="int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel (see IIP format)", required = false)
    ])
    def crop() {
        String fif = URLDecoder.decode(params.fif as String, grailsApplication.config.cytomine.ims.charset as String)
        String mimeType = params.mimeType
        NativeFormat imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType, true)

        int imageWidth = params.int('imageWidth')
        int imageHeight = params.int('imageHeight')
        def width = params.double('width')
        def height = params.double('height')
        def topLeftX = params.int('topLeftX')
        def topLeftY = params.int('topLeftY')

        def increaseArea = params.double('increaseArea', 1.0)
        if (increaseArea && increaseArea != 1.0) {
            width *= increaseArea
            height *= increaseArea
            topLeftX -=  width * (increaseArea - 1) / 2
            topLeftY +=  height * (increaseArea - 1) / 2
        }

        //we will increase the missing direction to make a square
        if (Boolean.parseBoolean(params.square)) {
            if (width < height) {
                double delta = height - width
                topLeftX -= delta / 2
                width += delta
            } else if (width > height) {
                double delta = width - height
                topLeftY += delta / 2
                height += delta
            }
        }

        width = Math.min(width, imageWidth)
        if (topLeftX < 0) {
            topLeftX = 0
        } else {
            topLeftX = Math.min((double) topLeftX, imageWidth - width)
        }

        height = Math.min(height, imageHeight)
        if (topLeftY > imageHeight) {
            topLeftY = imageHeight
        } else {
            topLeftY = Math.max((double) topLeftY, height)
        }

        // Read image from IIP
        params.width = width
        params.height = height
        params.topLeftX = topLeftX
        params.topLeftY = topLeftY
        String cropURL = imageFormat.cropURL(params)
        log.info cropURL
        int i = 0
        BufferedImage bufferedImage = ImageIO.read(new URL(cropURL))
        while (bufferedImage == null && i < 3) {
            bufferedImage = ImageIO.read(new URL(cropURL))
            i++
        }

        if (!bufferedImage) {
            throw new MiddlewareException("Not a valid image: ${cropURL}")
        }

        def type = params.type ?: 'crop'
        if (params.location) {
            Geometry geometry = new WKTReader().read(params.location as String)
            if (type == 'draw') {
                bufferedImage = imageProcessingService.createCropWithDraw(bufferedImage, geometry, params)
            }
            else if (type == 'mask') {
                bufferedImage = imageProcessingService.createMask(bufferedImage, geometry, params, false)
            }
            else if (type == 'alphaMask') {
                bufferedImage = imageProcessingService.createMask(bufferedImage, geometry, params, true)
            }
        }

//        if (params.boolean("point")) {
//            imageProcessingService.drawPoint(bufferedImage)
//        }

        if(params.boolean('drawScaleBar')) {
            /* If the crop mage has been resized, the image may be "cut" (how to know that?).
            (we may have oldWidth/oldHeight <> newWidth/newHeight)
            This mean that its impossible to compute the real size of the image because the size of the image change
            (not a problem) AND the image change (the image server cut some part of the image).
            I first try to compute the ratio (double ratioWidth = (double)((double)bufferedImage.getWidth()/params.double('width'))),
            but if the image is cut, its not possible to compute the good width size */

            Double resolution = params.double('resolution')
            Double magnification = params.double('magnification')
            bufferedImage = imageProcessingService.drawScaleBar(bufferedImage, width, resolution, magnification)
        }

        withFormat {
            png { responseBufferedImagePNG(bufferedImage) }
            jpg { responseBufferedImageJPG(bufferedImage) }
            tiff { responseBufferedImageTIFF(bufferedImage) }
        }
    }

    @RestApiMethod(description="Get a tile of an image", extensions = ["jpg"])
    @RestApiParams(params=[
            @RestApiParam(name="zoomify", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
            @RestApiParam(name="tileGroup", type="int", paramType = RestApiParamType.QUERY, description = "The Tile Group (see zoomify format)"),
            @RestApiParam(name="z", type="int", paramType = RestApiParamType.QUERY, description = "The Z index (see zoomify format)"),
            @RestApiParam(name="x", type="int", paramType = RestApiParamType.QUERY, description = "The X index (see zoomify format)"),
            @RestApiParam(name="y", type="int", paramType = RestApiParamType.QUERY, description = "The Y index (see zoomify format)"),
            @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
            @RestApiParam(name="tileIndex", type="int", paramType = RestApiParamType.QUERY, description = "The Tile Index (see IIP format)"),
            @RestApiParam(name="z", type="int", paramType = RestApiParamType.QUERY, description = "The Z index (see IIP format)"),
            @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file (see IIP format)"),
            @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed (see IIP format)"),
            @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast (see IIP format)"),
            @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction (see IIP format)")
    ])
    def tile() {
        String fif
        if (params.zoomify)
            fif = URLDecoder.decode(params.zoomify,"UTF-8")
        else
            fif = URLDecoder.decode(params.fif,"UTF-8")

        String mimeType = params.mimeType
        NativeFormat imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType, true)
        responseJPGImageFromUrl(imageFormat.tileURL(params))
    }


    //    @RestApiMethod(description="Extract a crop into an image")
//    @RestApiParams(params=[
//            @RestApiParam(name="cytomine", type="String", paramType = RestApiParamType.QUERY, description = " The URL of the related Cytomine-Core"),
//            @RestApiParam(name="name", type="String", paramType = RestApiParamType.QUERY, description = " The name of the generated image"),
//            @RestApiParam(name="storage", type="long", paramType = RestApiParamType.QUERY, description = "The id of the targeted storage"),
//            @RestApiParam(name="annotation", type="long", paramType = RestApiParamType.QUERY, description = "The id of the annotation"),
//            @RestApiParam(name="project", type="long", paramType = RestApiParamType.QUERY, description = " The id of the targeted project", required = false),
//    ])
//    def uploadCrop() {
//
//        String cytomineUrl =  params['cytomine']//grailsApplication.config.grails.cytomineUrl
//        String pubKey = grailsApplication.config.cytomine.ims.server.publicKey
//        String privKey = grailsApplication.config.cytomine.ims.server.privateKey
//
//        def user = cytomineService.tryAPIAuthentification(cytomineUrl,pubKey,privKey,request)
//        long currentUserId = user.id
//
//
//        log.info "init cytomine..."
//
//        Cytomine cytomine = new Cytomine((String) cytomineUrl, (String) user.publicKey, (String) user.privateKey)
//
//        def idStorage = Integer.parseInt(params['storage'] + "")
//
//        def parameters = cytomine.doGet("/api/annotation/"+params.annotation+"/cropParameters.json")
//
//        params.putAll(JSONValue.parse(parameters))
//
//        BufferedImage bufferedImage = readCropBufferedImage(params)
//
//        File output = new File("/tmp/"+params.name)
//        ImageIO.write(bufferedImage, "jpg", output)
//
//
//        def responseContent = [:]
//        responseContent.status = 200;
//        def uploadResult
//        try{
////            uploadResult = uploadService.upload(cytomine, output.name, idStorage, output.path, params['project']?[Integer.parseInt(params['project'] + "")]:null, currentUserId, null, new Date().getTime(), true)
//        } catch (Exception e){
//            e.printStackTrace()
//        }
//        responseContent.uploadFile = uploadResult.uploadedFile
//        responseContent.images = uploadResult.images
//        render responseContent as JSON
//    }
}
