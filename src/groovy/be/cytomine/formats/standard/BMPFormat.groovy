package be.cytomine.formats.standard

import grails.util.Holders
import utils.ServerUtils

/**
 * Created by stevben on 22/04/14.
 */
class BMPFormat extends CommonFormat {

    public BMPFormat() {
        extensions = ["bmp"]
        IMAGE_MAGICK_FORMAT_IDENTIFIER = "Format: BMP (Microsoft Windows bitmap image)"
        mimeType = "image/bmp"
        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerBase)
    }
}
