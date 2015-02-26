package be.cytomine.formats.standard

import grails.util.Holders
import org.springframework.util.StringUtils

/**
 * Created by hoyoux on 16.02.15.
 */
class PhotoshopTIFFFormat extends TIFFFormat {

    public boolean detect() {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        //we have a TIFF, but what kind ? flat, pyramid, multi-page, ventana ?

        return tiffinfo.contains("Adobe Photoshop CS3 Windows");
    }
}
