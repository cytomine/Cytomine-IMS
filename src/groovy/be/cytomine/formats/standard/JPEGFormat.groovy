package be.cytomine.formats.standard

/**
 * Created by stevben on 22/04/14.
 */
class JPEGFormat extends CommonFormat {

    public JPEGFormat () {
        extensions = ["jpg", "jpeg"]
        IMAGE_MAGICK_FORMAT_IDENTIFIER = "Format: JPEG (Joint Photographic Experts Group JFIF format)"
    }
}
