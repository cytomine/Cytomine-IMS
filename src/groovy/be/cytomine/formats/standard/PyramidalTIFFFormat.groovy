package be.cytomine.formats.standard

import org.springframework.util.StringUtils

/**
 * Created by stevben on 28/04/14.
 */
class PyramidalTIFFFormat  extends TIFFFormat {

    private excludeDescription = [
            "Not a TIFF",
            "<iScan",
            "Hamamatsu",
            "Aperio"
    ]

    public boolean detect() {
        String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)
        String tiffinfo = "tiffinfo $originalFilenameFullPath".execute().text
        //we have a TIFF, but what kind ? flat, pyramid, multi-page, ventana ?

        boolean notTiff = false
        excludeDescription.each {
            if (tiffinfo.contains(it)) notTiff |= true
        }
        if (notTiff) return false

        int nbTiffDirectory = StringUtils.countOccurrencesOf(tiffinfo, "TIFF Directory")

        if (nbTiffDirectory == 1) { //single layer tiff, we ne need to create a pyramid version
            return false
        } else if (nbTiffDirectory > 1) { //pyramid or multi-page
            return true //not sufficient probably
        }
    }
}
