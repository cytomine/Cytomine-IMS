package be.cytomine.formats.standard

/**
 * Created by stevben on 22/04/14.
 */
class PNGFormat extends CommonFormat {

    public PNGFormat() {
        extensions = ["png"]
        IMAGE_MAGICK_FORMAT_IDENTIFIER = "Format: PNG (Portable Network Graphics)"
    }
}
