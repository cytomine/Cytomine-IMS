package be.cytomine

import be.cytomine.client.Cytomine
import be.cytomine.client.models.AbstractImage
import be.cytomine.client.models.ImageInstance
import grails.converters.JSON
import org.openslide.AssociatedImage
import org.openslide.OpenSlide

import javax.imageio.ImageIO
import java.awt.Graphics2D
import java.awt.image.BufferedImage

class AbstractImageController {

    def cytomineService
    def imageProcessingService

    protected def responseFile(File file) {
        response.setHeader "Content-disposition", "attachment; filename=\"${file.getName()}\"";
        response.outputStream << file.newInputStream();
        response.outputStream.flush();
    }

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

    def download() {
        Cytomine cytomine = cytomineService.getCytomine()
        Long id = params.long("id")
        println "donwload $id"
        AbstractImage abstractImage = cytomine.getAbstractImage(id)
        String fullPath = abstractImage.getAt("fullPath")
        responseFile(new File(fullPath))
    }


    def label() {
        Cytomine cytomine = cytomineService.getCytomine()

        Long id = params.long("id")
        Integer maxWidth = params.int("maxWidth")
        String label = params.label

        AbstractImage abstractImage = cytomine.getAbstractImage(id)
        String fullPath = abstractImage.getAt("fullPath")
        String mime = abstractImage.getAt("mime")
        String[] mimeToRotate = ["scn", "mrxs"]

        File slideFile = new File(fullPath)
        if (slideFile.canRead()) {
            OpenSlide openSlide = new OpenSlide(slideFile)
            openSlide.getAssociatedImages().each {
                if (it.key == label) {
                    AssociatedImage associatedImage = it.value
                    BufferedImage bufferedImage = associatedImage.toBufferedImage()
                    if (mimeToRotate.contains(mime)) {
                        bufferedImage = imageProcessingService.rotate90ToRight(bufferedImage)
                    }
                    if (maxWidth && bufferedImage.width > maxWidth) {
                        int w = maxWidth
                        int h = bufferedImage.height / (bufferedImage.width / maxWidth)
                        bufferedImage = imageProcessingService.resizeImage(bufferedImage, w, h)
                    }
                    responseBufferedImage(bufferedImage)
                }
            }
            //label does not exists
            println "label $label does not exist for $fullPath"
        }
    }

    def associated() {
        Cytomine cytomine = cytomineService.getCytomine()

        Long id = params.long("id")

        AbstractImage abstractImage = cytomine.getAbstractImage(id)
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
