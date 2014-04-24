package be.cytomine.formats.standard

/**
 * Created by stevben on 22/04/14.
 */
class BMPFormat extends CommonFormat {

    public BMPFormat() {
        extensions = ["bmp"]
        IMAGE_MAGICK_FORMAT_IDENTIFIER = "Format: BMP (Microsoft Windows bitmap image)"
    }
}
