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

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Implement generics methods for handling imaging data in controllers
 */
class ImageResponseController {

    /**
     * Read a picture from url
     * @param url Picture url
     * @return Picture as an object
     */
    BufferedImage getImageFromURL(String url) {
        def out = new ByteArrayOutputStream()
        try {
            out << new URL(url).openStream()
        } catch (MalformedURLException | UnknownServiceException | java.io.IOException e) {
            log.error "getImageFromURL $url Exception " + e.toString()
        }
        InputStream inputStream = new ByteArrayInputStream(out.toByteArray())
        BufferedImage bufferedImage = ImageIO.read(inputStream)
        inputStream.close()
        out.close()
        return bufferedImage
    }

    def responseFile(File file) {
        BufferedInputStream bufferedInputStream = file.newInputStream()
        response.setHeader "Content-disposition", "attachment; filename=\"${file.getName()}\""
        response.outputStream << bufferedInputStream
        response.outputStream.flush()
        bufferedInputStream.close()
    }

    /**
     * Response an image as a HTTP response
     * @param bufferedImage Image
     */
    def responseBufferedImage(BufferedImage bufferedImage) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        withFormat {
            png {
                if (request.method == 'HEAD') {
                    render(text: "", contentType: "image/png")
                }
                else {
                    ImageIO.write(bufferedImage, "png", baos);
                    byte[] bytesOut = baos.toByteArray();
                    response.contentLength = baos.size();
                    response.setHeader("Connection", "Keep-Alive")
                    response.setHeader("Accept-Ranges", "bytes")
                    response.setHeader("Content-Type", "image/png")
                    response.getOutputStream() << bytesOut
                    response.getOutputStream().flush()
                }
            }
            jpg {
                if (request.method == 'HEAD') {
                    render(text: "", contentType: "image/jpeg");
                }
                else {
                    ImageIO.write(bufferedImage, "jpg", baos);
                    byte[] bytesOut = baos.toByteArray();
                    response.contentLength = baos.size();
                    response.setHeader("Connection", "Keep-Alive")
                    response.setHeader("Accept-Ranges", "bytes")
                    response.setHeader("Content-Type", "image/jpeg")
                    response.getOutputStream() << bytesOut
                    response.getOutputStream().flush()
                }
            }
        }
        baos.close()
    }

    /**
     * Response an image as a HTTP response
     * @param url Image url
     */
    def responseImageFromUrl(String url) {
        log.info "url=$url"
        URL source = new URL(url)
        URLConnection connection = source.openConnection()
        response.contentType = 'image/jpeg'
        // Set the content length
        response.setHeader("Content-Length", connection.contentLength.toString())
        // Get the input stream from the connection
        InputStream is = connection.getInputStream()
        response.outputStream << is
        response.outputStream.flush()
        is.close()
    }
}
