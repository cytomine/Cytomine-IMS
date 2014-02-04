package be.cytomine

import be.cytomine.client.Cytomine
import be.cytomine.client.models.AbstractImage
import be.cytomine.client.models.ImageInstance
import grails.converters.JSON
import org.openslide.AssociatedImage
import org.openslide.OpenSlide

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

class ImageController {

    def cytomineService

    protected def responseBufferedImage(BufferedImage bufferedImage) {
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
    }

    def label() {
        Cytomine cytomine = cytomineService.getCytomine()

        Long id = params.long("id")
        String label = params.label

        AbstractImage abstractImage = cytomine.getImage(id)
        String fullPath = abstractImage.getAt("fullPath")

        File slideFile = new File(fullPath)
        if (slideFile.canRead()) {
            OpenSlide openSlide = new OpenSlide(slideFile)
            openSlide.getAssociatedImages().each {
                if (it.key == label) {
                    AssociatedImage associatedImage = it.value
                    responseBufferedImage(associatedImage.toBufferedImage())
                }
            }
        }
    }

    def associated() {
        Cytomine cytomine = cytomineService.getCytomine()

        Long id = params.long("id")

        AbstractImage abstractImage = cytomine.getImage(id)
        String fullPath = abstractImage.getAt("fullPath")

        File slideFile = new File(fullPath)
        if (slideFile.canRead()) {
            println fullPath
            OpenSlide openSlide = new OpenSlide(slideFile)
            def labels = openSlide.getAssociatedImages().collect { it.key }
            render labels as JSON
        }
    }
}
