package be.cytomine.image

import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.ImageFormat
import grails.converters.JSON

import java.awt.image.BufferedImage

class ImageController extends ImageUtilsController {

    def thumb() {
        String fif = params.fif
        int maxSize = params.int('maxSize', 256)
        ImageFormat[] imageFormats = FormatIdentifier.getImageFormats(fif)
        println imageFormats
        assert(imageFormats.size() == 1)
        BufferedImage bufferedImage = imageFormats.first().thumb(maxSize)
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

    def crop() {
        String fif = params.fif
        ImageFormat[] imageFormats = FormatIdentifier.getImageFormats(fif)
        println imageFormats
        assert(imageFormats.size() == 1)
        String cropURL = imageFormats.first().cropURL(params)
        responseImageFromUrl(cropURL)
    }

    def tile() {
        String fif = params.zoomify
        /*remove the "/" at the end of the path injected by openlayers (OL2).
          I Did not find the way to avoid it from OL2 (BS)
         */
        fif = fif.substring(0, fif.length()-1)
        String tileURL = ImageFormat.tileURL(fif, params)
        responseImageFromUrl(tileURL)

    }


}
