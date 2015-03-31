package be.cytomine.formats.specialtiff

import be.cytomine.formats.standard.TIFFFormat
import grails.util.Holders
import org.springframework.util.StringUtils
import utils.ServerUtils

/**
 * Created by hoyoux on 16.02.15.
 */
class PhotoshopTIFFFormat extends TIFFToConvert {

    public PhotoshopTIFFFormat () {
        extensions = ["tif", "tiff"]
        //mimeType = "image/pyrtiff"
        //iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerBase)
    }

    public boolean detect() {
        String tiffinfo = getTiffInfo()
        //we have a TIFF, but what kind ? flat, pyramid, multi-page, ventana ?

        return tiffinfo.contains("Adobe Photoshop CS3 Windows");
    }
}
