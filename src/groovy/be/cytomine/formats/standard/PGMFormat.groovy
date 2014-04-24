package be.cytomine.formats.standard

/**
 * Created by stevben on 22/04/14.
 */
class PGMFormat extends CommonFormat {

    public PGMFormat () {
        extensions = ["pgm"]
        IMAGE_MAGICK_FORMAT_IDENTIFIER = "Format: PGM (Portable graymap format"
    }
}
