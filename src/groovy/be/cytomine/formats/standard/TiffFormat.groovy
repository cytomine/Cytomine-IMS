package be.cytomine.formats.standard

import grails.util.Holders

import java.awt.image.BufferedImage

/**
 * Created by stevben on 22/04/14.
 */
abstract class TIFFFormat extends CommonFormat {

    public TIFFFormat() {
        extensions = ["tif", "tiff"]
        mimeType = "image/tiff"
<<<<<<< Updated upstream
    }

    def properties() {
        def tiffinfoExecutable = Holders.config.grails.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        def properties = []
        int maxWidth = 0
        int maxHeight = 0
        tiffinfo.tokenize( '\n' ).findAll {
            it.contains 'Image Width:'
        }.each {
            def tokens = it.tokenize(" ")
            int width = Integer.parseInt(tokens.get(2))
            int height = Integer.parseInt(tokens.get(5))
            maxWidth = Math.max(maxWidth, width)
            maxHeight = Math.max(maxHeight, height)
        }
        properties << [ key : "cytomine.width", value : maxWidth ]
        properties << [ key : "cytomine.height", value : maxHeight ]
        properties << [ key : "cytomine.resolution", value : null ]
        properties << [ key : "cytomine.magnification", value : null ]
        return properties

    }

    BufferedImage associated(String label) {
        if (label == "macro") {
            return thumb(256)
        }
        if (label == "preview") {
            return thumb(1024)
        }
    }

    BufferedImage thumb(int maxSize) {
        getTIFFSubImage(0)
=======
>>>>>>> Stashed changes
    }

}
