package be.cytomine.image

/*
 * Copyright (c) 2009-2020. Authors: see NOTICE file.
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

import be.cytomine.exception.ObjectNotFoundException
import be.cytomine.formats.Format
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.tools.CytomineFile
import be.cytomine.formats.tools.MultipleFilesFormat

import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType
import utils.ImageUtils
import utils.MimeTypeUtils

import java.awt.image.BufferedImage
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RestApi(name = "image services", description = "Methods for images (properties, associated images, ...)")
class ImageController extends ImageResponseController {

    def imageProcessingService

    @RestApiMethod(description = "Get the available properties (with, height, resolution, magnitude, ...) of an image", extensions = ["json"])
    @RestApiParams(params = [
            @RestApiParam(name = "fif", type = "String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name = "mimeType", type = "String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
    ])
    def properties() {
        String fif = URLDecoder.decode(params.fif, "UTF-8")
        Format imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify()
        render imageFormat.cytomineProperties() as JSON
    }

    @RestApiMethod(description = "Get the list of nested (or associated) images available of an image", extensions = ["json"])
    @RestApiParams(params = [
            @RestApiParam(name = "fif", type = "String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name = "mimeType", type = "String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
    ])
    def associated() {
        String fif = URLDecoder.decode(params.fif, "UTF-8")
        String mimeType = params.mimeType
        Format imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType)
        render imageFormat.associated() as JSON
    }

    @RestApiMethod(description = "Get a nested (or associated) image (e.g. macro) of an image", extensions = ["jpg", "png"])
    @RestApiParams(params = [
            @RestApiParam(name = "fif", type = "String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name = "mimeType", type = "String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
            @RestApiParam(name = "label", type = "String", paramType = RestApiParamType.QUERY, description = "The requested nested image, identified by label (e.g. macro)"),
            @RestApiParam(name = "maxSize", type = "int", paramType = RestApiParamType.QUERY, description = " The max width or height of the generated thumb", required = false)
    ])
    def nested() {
        String fif = URLDecoder.decode(params.fif, "UTF-8")
        String label = params.label
        String mimeType = params.mimeType
        int maxSize = params.int('maxSize', 512)
        params.maxSize = maxSize
        Format imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType)
        BufferedImage bufferedImage = imageFormat.associated(label)
        if (bufferedImage) {
            bufferedImage = imageProcessingService.scaleImage(bufferedImage, maxSize, maxSize)
            withFormat {
                png { responseBufferedImagePNG(bufferedImage) }
                jpg { responseBufferedImageJPG(bufferedImage) }
                tiff { responseBufferedImageTIFF(bufferedImage) }
            }
        } else {
            throw new ObjectNotFoundException(params.label + " not found")
        }
    }

    @RestApiMethod(description = "Download an image")
    @RestApiParams(params = [
            @RestApiParam(name = "fif", type = "String", paramType = RestApiParamType.QUERY, description = "The absolute path of the image"),
            @RestApiParam(name = "mimeType", type = "String", paramType = RestApiParamType.QUERY, description = "The mime type of the image"),
    ])
    def download() {
        String fif = URLDecoder.decode(params.fif, "UTF-8")
        String mimeType = params.mimeType

        File file = new File(fif)
        if (!mimeType) {
            responseFile(file)
            return
        }

        Format format = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType)
        if (format instanceof MultipleFilesFormat) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            ZipOutputStream zipFile = new ZipOutputStream(baos)

            File current = file.parentFile

            Deque<File> queue = new LinkedList<File>()
            queue.push(current)
            URI base = current.toURI()

            while (!queue.isEmpty()) {
                current = queue.pop()
                for (File kid : current.listFiles()) {
                    String name = base.relativize(kid.toURI()).getPath()
                    if (kid.isDirectory()) {
                        zipFile.putNextEntry(new ZipEntry(name + "/"))
                        queue.push(kid)
                    } else {
                        zipFile.putNextEntry(new ZipEntry(name))
                        zipFile << kid.bytes
                        zipFile.closeEntry()
                    }
                }
            }

            zipFile.finish()

            String filename
            if (file.name.lastIndexOf('.') > -1)
                filename = file.name.substring(0, file.name.lastIndexOf('.')) + ".zip"
            else
                filename = file.name + ".zip"

            response.setHeader "Content-disposition", "attachment; filename=\"${filename}\""
            response.setContentType(MimeTypeUtils.MIMETYPE_ZIP)
            response.outputStream << baos.toByteArray()
            response.outputStream.flush()
            zipFile.close()
        }
        else {
            responseFile(file)
        }
    }
}