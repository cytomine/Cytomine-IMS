package be.cytomine.formats

import be.cytomine.client.Cytomine
import be.cytomine.client.models.AbstractImage
import be.cytomine.ImageController
import grails.converters.JSON
import org.openslide.AssociatedImage
import org.openslide.OpenSlide

import java.awt.image.BufferedImage

class OpenSlideController extends ImageController{

    def cytomineService
    def imageProcessingService

    //put into service
    def label() {
        Cytomine cytomine = cytomineService.getCytomine(params.cytomineUrl)

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

    //put into service
    def associated() {
        Cytomine cytomine = cytomineService.getCytomine(params.cytomineUrl)

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
