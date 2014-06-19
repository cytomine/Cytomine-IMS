package be.cytomine.image

import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.ImageFormat
import grails.converters.JSON

import java.awt.image.BufferedImage

class ImageController extends ImageUtilsController {

    def thumb() {
        String fif = params.fif
        int maxSize = params.int('maxSize')
        ImageFormat[] imageFormats = FormatIdentifier.getImageFormats(fif)
        println imageFormats
        assert(imageFormats.size() == 1)
        BufferedImage bufferedImage = imageFormats.first().thumb(maxSize)
        println "bufferedImage = " + bufferedImage
        if (bufferedImage) {
            responseBufferedImage(bufferedImage)
        } else {
            //return 404 image
        }
    }

    def nested() {
        String fif = params.fif
        String label = params.label
        ImageFormat[] imageFormats = FormatIdentifier.getImageFormats(fif)
        assert(imageFormats.size() == 1)
        BufferedImage bufferedImage = imageFormats.first().associated(label)
        println bufferedImage
        if (bufferedImage) {
            responseBufferedImage(bufferedImage)
        } else {
            //return 404 image
        }

    }

    def associated() {
        String fif = params.fif
        ImageFormat[] imageFormats = FormatIdentifier.getImageFormats(fif)
        println imageFormats
        assert(imageFormats.size() == 1)
        render imageFormats.first().associated() as JSON
    }

    def properties() {
        String fif = params.fif
        ImageFormat[] imageFormats = FormatIdentifier.getImageFormats(fif)
        println imageFormats
        assert(imageFormats.size() == 1)
        render imageFormats.first().properties() as JSON
    }

    def roi() {
        def url = "http://localhost:8081/fcgi-bin/iipsrv.fcgi?"
        params.each {
            url += "$it.key=$it.value&"
        }
        responseImageFromUrl(url)
    }


}
