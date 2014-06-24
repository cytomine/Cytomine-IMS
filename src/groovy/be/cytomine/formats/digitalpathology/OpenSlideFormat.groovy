package be.cytomine.formats.digitalpathology

import be.cytomine.formats.ImageFormat
import org.openslide.AssociatedImage
import org.openslide.OpenSlide

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 22/04/14.
 */
abstract class OpenSlideFormat extends ImageFormat {

    protected String vendor = null

    boolean detect() {
        println "detect $absoluteFilePath"
        File slideFile = new File(absoluteFilePath)
        if (slideFile.canRead()) {
            println "can read $absoluteFilePath " +  OpenSlide.detectVendor(slideFile)
            try {
                return OpenSlide.detectVendor(slideFile) == vendor
            } catch (java.io.IOException e) {
                //Not a file that OpenSlide can recognize
                return false
            }
        } else {
            //throw ERROR reading file
        }

    }

    String convert(String workingPath) {
        return null //nothing to do, the format is understood by IIP+OpenSlide natively
    }

    public BufferedImage associated(String label) { //should be abstract
        File slideFile = new File(absoluteFilePath)
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
        else return null
    }

    public def properties() {
        File slideFile = new File(absoluteFilePath)
        def properties = [[key : "mimeType", value : mimeType]]
        if (slideFile.canRead()) {
            OpenSlide openSlide = new OpenSlide(slideFile)
            openSlide.getProperties().each {
                properties << [ key : it.key, value  : it.value]
            }
        }

        println properties
        if (widthProperty)
            properties << [ key : "cytomine.width", value : Integer.parseInt(properties.find { it.key == widthProperty}?.value) ]
        if (heightProperty)
            properties << [ key : "cytomine.height", value : Integer.parseInt(properties.find { it.key == heightProperty}?.value) ]
        if (resolutionProperty)
            properties << [ key : "cytomine.resolution", value : Double.parseDouble(properties.find { it.key == resolutionProperty}?.value) ]
        if (magnificiationProperty)
            properties << [ key : "cytomine.magnification", value : Double.parseDouble(properties.find { it.key == magnificiationProperty}?.value).intValue() ]

        return properties
    }

    public BufferedImage thumb(int maxSize) {
        OpenSlide openSlide = new OpenSlide(new File(absoluteFilePath))
        return openSlide.createThumbnailImage(maxSize)
    }


}
