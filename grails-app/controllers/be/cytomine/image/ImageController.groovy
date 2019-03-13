package be.cytomine.image

import be.cytomine.client.Cytomine
import be.cytomine.exception.DeploymentException
import be.cytomine.exception.ObjectNotFoundException

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
import be.cytomine.formats.supported.SupportedImageFormat
import be.cytomine.exception.MiddlewareException
import be.cytomine.formats.supported.digitalpathology.OpenSlideMultipleFileFormat
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.io.WKTReader
import grails.converters.JSON
import grails.util.Holders
import org.json.simple.JSONValue
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RestApi(name = "image services", description = "Methods for images (thumb, tile, property, ...)")
class ImageController extends ImageUtilsController {

    def imageProcessingService
    def tileService
    def uploadService
    def cytomineService

    @RestApiMethod(description="Get the thumb of an image", extensions = ["jpg","png"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name="mimeType", type="String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
            @RestApiParam(name="maxSize", type="int", paramType = RestApiParamType.QUERY, description = "The max width or height of the generated thumb", required = false)
    ])
    def thumb() {
        String fif = URLDecoder.decode(params.fif,"UTF-8")
        int maxSize = params.int('maxSize', 512)
        String mimeType = params.mimeType
        SupportedImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
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
        String fif = URLDecoder.decode(params.fif,"UTF-8")
        String label = params.label
        String mimeType = params.mimeType
        int maxSize = params.int('maxSize', 512)
        SupportedImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
        log.info "imageFormat=${imageFormat.class}"
        BufferedImage bufferedImage = imageFormat.associated(label)
        if (bufferedImage) {
            bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
            responseBufferedImage(bufferedImage)
        } else {
            throw new ObjectNotFoundException(params.label+" not found")
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
        SupportedImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
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
            @RestApiParam(name="draw", type="boolean", paramType = RestApiParamType.QUERY, description = " If used, draw the geometry contour on the crop. draw takes precedence over mask & alphamask.", required = false),
            @RestApiParam(name="thickness", type="int", paramType = RestApiParamType.QUERY, description = " If draw used, set the thickness of the geometry contour on the crop.", required = false),
            @RestApiParam(name="color", type="String", paramType = RestApiParamType.QUERY, description = " If draw used, set the color of the geometry contour on the crop.", required = false),
            @RestApiParam(name="square", type="boolean", paramType = RestApiParamType.QUERY, description = " If draw used, try to extends the ROI around the crop to have a square.", required = false),
            @RestApiParam(name="mask", type="int", paramType = RestApiParamType.QUERY, description = " If used, return the mask of the geometry (black & white) instead of the crop. mask takes precedence over alphamask", required = false),
            @RestApiParam(name="alphaMask", type="int", paramType = RestApiParamType.QUERY, description = " If used, return the crop with the mask in the alphachannel (0% to 100%). PNG required", required = false),
    ])
    def crop() {

        def savedWidth = params.double('width')
        def savedHeight = params.double('height')

        SupportedImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(URLDecoder.decode(params.fif,"UTF-8"), params.mimeType)

        BufferedImage bufferedImage = readCropBufferedImage(params)

        if(params.boolean("point")) {
            drawPoint(bufferedImage)
        }


        if (params.draw) {
            String location = params.location
            Geometry geometry = new WKTReader().read(location)
            bufferedImage = imageProcessingService.createCropWithDraw(bufferedImage, geometry, params)
        } else if (params.mask) {
            String location = params.location
            Geometry geometry  = new WKTReader().read(location)
            bufferedImage = imageProcessingService.createMask(bufferedImage, geometry, params, false)
        } else if (params.alphaMask) {
            String location = params.location
            Geometry geometry = new WKTReader().read(location)

            if (params.zoom) {
                int zoom = params.int('zoom', 0)
                int maxWidth = savedWidth / Math.pow(2, zoom)
                int maxHeight = savedHeight / Math.pow(2, zoom)
                //resize and preserve png transparency for alpha mask
//            bufferedImage = Scalr.resize(bufferedImage,  Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH,
//                    maxWidth, maxHeight, Scalr.OP_ANTIALIAS);
                // Create new (blank) image of required (scaled) size

                bufferedImage = ImageUtils.resize(bufferedImage, maxWidth, maxHeight)
            }


            bufferedImage = imageProcessingService.createMask(bufferedImage, geometry, params, true)
        }

        if (params.zoom && !params.alphaMask) {
            int zoom = params.int('zoom', 0)
            int maxWidth = savedWidth / Math.pow(2, zoom)
            int maxHeight = savedHeight / Math.pow(2, zoom)

            bufferedImage = ImageUtils.resize(bufferedImage, maxWidth, maxHeight)
        }

        if(params.boolean('drawScaleBar')) {
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

    @RestApiMethod(description="Extract a crop into an image")
    @RestApiParams(params=[
            @RestApiParam(name="cytomine", type="String", paramType = RestApiParamType.QUERY, description = " The URL of the related Cytomine-Core"),
            @RestApiParam(name="name", type="String", paramType = RestApiParamType.QUERY, description = " The name of the generated image"),
            @RestApiParam(name="storage", type="long", paramType = RestApiParamType.QUERY, description = "The id of the targeted storage"),
            @RestApiParam(name="annotation", type="long", paramType = RestApiParamType.QUERY, description = "The id of the annotation"),
            @RestApiParam(name="project", type="long", paramType = RestApiParamType.QUERY, description = " The id of the targeted project", required = false),
    ])
    def uploadCrop() {

        String cytomineUrl =  params['cytomine']//grailsApplication.config.grails.cytomineUrl
        String pubKey = grailsApplication.config.cytomine.imageServerPublicKey
        String privKey = grailsApplication.config.cytomine.imageServerPrivateKey

        def user = cytomineService.tryAPIAuthentification(cytomineUrl,pubKey,privKey,request)
        long currentUserId = user.id


        log.info "init cytomine..."

        Cytomine cytomine = new Cytomine((String) cytomineUrl, (String) user.publicKey, (String) user.privateKey)

        def idStorage = Integer.parseInt(params['storage'] + "")

        def parameters = cytomine.doGet("/api/annotation/"+params.annotation+"/cropParameters.json")

        params.putAll(JSONValue.parse(parameters))

        BufferedImage bufferedImage = readCropBufferedImage(params)

        File output = new File("/tmp/"+params.name)
        ImageIO.write(bufferedImage, "jpg", output)


        def responseContent = [:]
        responseContent.status = 200;
        def uploadResult
        try{
            uploadResult = uploadService.upload(cytomine, output.name, idStorage, output.path, params['project']?[Integer.parseInt(params['project'] + "")]:null, currentUserId, null, new Date().getTime(), true)
        } catch (Exception e){
            e.printStackTrace()
        }
        responseContent.uploadFile = uploadResult.uploadedFile
        responseContent.images = uploadResult.images
        render responseContent as JSON
    }
    public void drawPoint(BufferedImage image) {
        Graphics g = image.createGraphics();
        g.setColor(Color.RED);

        int length = 10
        int x = image.getWidth()/2
        int y = image.getHeight()/2

        g.setStroke(new BasicStroke(1));
        g.drawLine(x, y-length, x, y+length);
        g.drawLine(x-length,y,x+length,y);
        g.dispose();
    }

    public BufferedImage readCropBufferedImage(def params) {
        String fif = params.fif
        String mimeType = params.mimeType

        SupportedImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)

        int width = params.int('width')
        int height = params.int('height')
        int topLeftX = params.int('topLeftX')
        int topLeftY = params.int('topLeftY')
        int imageWidth = Integer.parseInt(params.imageWidth)
        int imageHeight = Integer.parseInt(params.imageHeight)

        if (params.increaseArea) {
            double increaseArea = params.double("increaseArea")
            topLeftX -= width * (increaseArea - 1) / 2
            topLeftY += height * (increaseArea - 1) / 2
            width *= increaseArea
            height *= increaseArea
        }

        //we will increase the missing direction to make a square
        if(Boolean.parseBoolean(params.square)){
            if(width < height) {
                double delta = height - width
                topLeftX -= delta/2
                width += delta

                if(topLeftX < 0) {
                    topLeftX = 0
                } else {
                    topLeftX = Math.min(topLeftX, imageWidth - width)
                }
            } else if(width > height) {
                double delta = width - height
                topLeftY += delta/2
                height += delta

                if(topLeftY > imageHeight){
                    topLeftY = imageHeight
                }
                else {
                    topLeftY = Math.max(topLeftY, height)
                }
            }
        }

        params.topLeftX = topLeftX.toString()
        params.topLeftY = topLeftY.toString()
        params.width = width.toString()
        params.height = height.toString()

        log.info(params)
        String cropURL = imageFormat.cropURL(params, grailsApplication.config.cytomine.charset)
        log.info cropURL

        BufferedImage bufferedImage = ImageIO.read(new URL(cropURL))

        int i = 0
        while (bufferedImage == null && i < 3) {
            bufferedImage = ImageIO.read(new URL(cropURL))
            i++
        }

        if (bufferedImage == null) {
            throw new MiddlewareException("Not a valid image: ${cropURL}")
        }

        if (params.safe) {
            //if safe mode, skip annotation too large
            if ((params.int('width') > grailsApplication.config.cytomine.maxAnnotationOnImageWidth) ||
                    (params.int('height') > grailsApplication.config.cytomine.maxAnnotationOnImageWidth)){
                throw new MiddlewareException("Too big annotation!");
            }
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
    }

    @RestApiMethod(description="Download an image", extensions = ["jpg","png"])
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
        SupportedImageFormat format = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)

        if(format instanceof OpenSlideMultipleFileFormat) {

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
