package be.cytomine.formats

import org.openslide.AssociatedImage
import org.openslide.OpenSlide

import java.awt.image.BufferedImage

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
        BufferedImage associatedBufferedImage = null
        if (slideFile.canRead()) {
            OpenSlide openSlide = new OpenSlide(slideFile)
            openSlide.getAssociatedImages().each {
                if (it.key == label) {
                    AssociatedImage associatedImage = it.value
                    associatedBufferedImage = associatedImage.toBufferedImage()
                }
            }
        }
        if (associatedBufferedImage) return associatedBufferedImage
        else println "label $label does not exist for $fullPath" //label does not exists
    }

}
