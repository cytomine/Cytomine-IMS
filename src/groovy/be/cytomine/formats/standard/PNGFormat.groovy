package be.cytomine.formats.standard

import grails.util.Holders
import utils.ServerUtils

/**
 * Created by stevben on 22/04/14.
 */
class PNGFormat extends CommonFormat {

    public PNGFormat() {
        extensions = ["png"]
        IMAGE_MAGICK_FORMAT_IDENTIFIER = "Format: PNG (Portable Network Graphics)"
        mimeType = "image/png"
        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerBase)
    }
}
