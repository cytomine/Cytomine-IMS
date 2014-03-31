package be.cytomine.formats

import grails.transaction.Transactional
import org.openslide.AssociatedImage
import org.openslide.OpenSlide

import java.awt.image.BufferedImage

@Transactional
class OpenslideService {

   public String getAssociatedImages(String fullPath) {
        File slideFile = new File(fullPath)
        if (slideFile.canRead()) {
            println fullPath
            OpenSlide openSlide = new OpenSlide(slideFile)
            println "detectVendor " + openSlide.detectVendor()
            return openSlide.getAssociatedImages().collect { it.key }
        } else return []
    }

    public BufferedImage getAssociatedImage(String fullPath, String label) {
        File slideFile = new File(fullPath)
        if (slideFile.canRead()) {
            OpenSlide openSlide = new OpenSlide(slideFile)
            openSlide.getAssociatedImages().each {
                if (it.key == label) {
                    AssociatedImage associatedImage = it.value
                    return associatedImage.toBufferedImage()
                }
            }
            //label does not exists
            println "label $label does not exist for $fullPath"
        }
    }

}
