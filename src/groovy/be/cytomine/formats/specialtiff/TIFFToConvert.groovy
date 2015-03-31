package be.cytomine.formats.specialtiff

import be.cytomine.formats.standard.TIFFFormat
import grails.util.Holders

/**
 * Created by hoyoux on 31.03.15.
 */
class TIFFToConvert extends TIFFFormat {

    public String getTiffInfo() {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        return tiffinfo;
    }
}
