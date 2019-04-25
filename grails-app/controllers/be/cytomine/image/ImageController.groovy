package be.cytomine.image

import be.cytomine.formats.tools.CytomineFile

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

import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.tools.MultipleFilesFormat
import be.cytomine.formats.supported.NativeFormat
import be.cytomine.exception.MiddlewareException
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RestApi(name = "image services", description = "Methods for images (thumb, tile, property, ...)")
class ImageController extends ImageResponseController {

    def imageProcessingService
    def tileService
    def uploadService
    def cytomineService

    @RestApiMethod(description="Get the thumb of an image", extensions = ["jpg","png", "tiff"])
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
        log.info (params)
        BufferedImage bufferedImage = imageFormat.thumb(params)
        bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
        if (bufferedImage) {
            withFormat {
                png { responseBufferedImagePNG(bufferedImage) }
                jpg { responseBufferedImageJPG(bufferedImage) }
                tiff { responseBufferedImageTIFF(bufferedImage) }
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
        String fif = URLDecoder.decode(params.fif,"UTF-8")
        String label = params.label
        String mimeType = params.mimeType
        int maxSize = params.int('maxSize', 512)
        params.maxSize = maxSize
        NativeFormat imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType, true)
        log.info "imageFormat=${imageFormat.class}"
        BufferedImage bufferedImage = imageFormat.associated(label)
        bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
        if (bufferedImage) {
            withFormat {
                png { responseBufferedImagePNG(bufferedImage) }
                jpg { responseBufferedImageJPG(bufferedImage) }
                tiff { responseBufferedImageTIFF(bufferedImage) }
            }
        }
    }

    @RestApiMethod(description="Get the list of nested (or associated) images available of an image", extensions = ["json"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
    ])
    def associated() {
        String fif = URLDecoder.decode(params.fif,"UTF-8")
        String mimeType = params.mimeType
        NativeFormat imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType, true)
        render imageFormat.associated() as JSON
    }

    @RestApiMethod(description="Get the available properties (with, height, resolution, magnitude, ...) of an image", extensions = ["json"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
    ])
    def properties() {
        String fif = URLDecoder.decode(params.fif,"UTF-8")
        String mimeType = params.mimeType
        NativeFormat imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType, true)
        render imageFormat.properties() as JSON
    }

    @RestApiMethod(description="Get the crop of an image", extensions = ["jpg", "png", "tiff"])
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
            @RestApiParam(name="draw", type="int", paramType = RestApiParamType.QUERY, description = " If used, draw the geometry contour on the crop. draw takes precedence over mask & alphamask.", required = false),
            @RestApiParam(name="mask", type="int", paramType = RestApiParamType.QUERY, description = " If used, return the mask of the geometry (black & white) instead of the crop. mask takes precedence over alphamask", required = false),
            @RestApiParam(name="alphaMask", type="int", paramType = RestApiParamType.QUERY, description = " If used, return the crop with the mask in the alphachannel (0% to 100%). PNG required", required = false),
            @RestApiParam(name="drawScaleBar", type="int", paramType = RestApiParamType.QUERY, description = "If true, draw a scale bar", required = false),
            @RestApiParam(name="resolution", type="float", paramType = RestApiParamType.QUERY, description = "Resolution to print in scale bar if used", required=false),
            @RestApiParam(name="magnification", type="float", paramType = RestApiParamType.QUERY, description = "Magnification to print in scale bar if used", required=false),
            @RestApiParam(name="increaseArea", type="boolean",  paramType = RestApiParamType.QUERY, description = "", required=false),
            @RestApiParam(name="safe", type="boolean", paramType = RestApiParamType.QUERY, description = "If true, skip too large ROI", required=false),
            @RestApiParam(name="colormap", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of a colormap file (see IIP format)", required = false),
            @RestApiParam(name="inverse", type="int", paramType = RestApiParamType.QUERY, description = "True if colors have to be inversed (see IIP format)", required = false),
            @RestApiParam(name="contrast", type="float", paramType = RestApiParamType.QUERY, description = "Multiply pixels by contrast (see IIP format)", required = false),
            @RestApiParam(name="gamma", type="float", paramType = RestApiParamType.QUERY, description = "Apply gamma correction (see IIP format)", required = false),
            @RestApiParam(name="bits", type="int", paramType = RestApiParamType.QUERY, description = "Output bit depth per channel (see IIP format)", required = false)
    ])
    def crop() {
        String fif = URLDecoder.decode(params.fif as String, grailsApplication.config.cytomine.charset as String)
        String mimeType = params.mimeType
        NativeFormat imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType, true)

        def increaseArea = params.double('increaseArea', 1.0)
        def savedWidth = params.double('width')
        def savedHeight = params.double('height')

        def width = params.double('width')
        def height = params.double('height')
        def topLeftX = params.int('topLeftX')
        def topLeftY = params.int('topLeftY')

        if (increaseArea && increaseArea != 1.0) {
            width *= increaseArea
            height *= increaseArea
            topLeftX -= ((width - savedWidth) / 2)
            topLeftY += ((height - savedHeight) / 2)
        }

        def safe = params.boolean('safe', false)
        if (safe) {
            //if safe mode, skip annotation too large
            if (width > grailsApplication.config.cytomine.maxAnnotationOnImageWidth ||
                    height > grailsApplication.config.cytomine.maxAnnotationOnImageWidth) {
                throw new MiddlewareException("Requested area is too big.")
            }
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
//        String pubKey = grailsApplication.config.cytomine.imageServerPublicKey
//        String privKey = grailsApplication.config.cytomine.imageServerPrivateKey
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
        String fif = URLDecoder.decode(params.get("fif"),"UTF-8")
        String mimeType = params.get("mimeType")
        File file = new File(fif)

        if(!mimeType) {
            responseFile(file)
            return
        }
        NativeFormat imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType, true)

        if(format instanceof MultipleFilesFormat) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            ZipOutputStream zipFile = new ZipOutputStream(baos)

            File current = file.parentFile

            Deque<File> queue = new LinkedList<File>();
            queue.push(current)
            URI base = current.toURI();

            while(!queue.isEmpty()){
                current = queue.pop()
                for (File kid : current.listFiles()) {
                    String name = base.relativize(kid.toURI()).getPath();
                    if(kid.isDirectory()){
                        zipFile.putNextEntry(new ZipEntry(name+"/"))
                        queue.push(kid)
                    } else {
                        zipFile.putNextEntry(new ZipEntry(name));
                        zipFile << kid.bytes
                        zipFile.closeEntry();
                    }
                }
            }

            zipFile.finish();
            response.setContentType("APPLICATION/ZIP");
            String filename;
            if(file.name.lastIndexOf('.') > -1)
                filename = file.name.substring(0,file.name.lastIndexOf('.'))+".zip"
            else
                filename = file.name+".zip"

            response.setHeader "Content-disposition", "attachment; filename=\"${filename}\""
            response.outputStream << baos.toByteArray()
            response.outputStream.flush()
            zipFile.close()
        } else {
            responseFile(file)
        }
    }
}