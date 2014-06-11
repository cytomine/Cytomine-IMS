package be.cytomine.formats.standard

import grails.util.Holders
import org.springframework.util.StringUtils

/**
 * Created by stevben on 28/04/14.
 */
class PlanarTIFFFormat extends TIFFFormat {

    private excludeDescription = [
            "Not a TIFF",
            "<iScan",
            "Make: Hamamatsu",
            "Leica",
            "ImageDescription: Aperio Image Library"
    ]

    public boolean detect() {
        def tiffinfoExecutable = Holders.config.grails.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        //we have a TIFF, but what kind ? flat, pyramid, multi-page, ventana ?

        boolean notTiff = false
        excludeDescription.each {
            notTiff |= tiffinfo.contains(it)
        }
        if (notTiff) return false

        int nbTiffDirectory = StringUtils.countOccurrencesOf(tiffinfo, "TIFF Directory")

        return (nbTiffDirectory == 1) //single layer tiff, we ne need to create a pyramid version
    }
}
