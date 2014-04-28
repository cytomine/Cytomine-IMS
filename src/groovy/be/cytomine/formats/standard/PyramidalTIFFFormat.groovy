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
            "Aperio",
            "Leica"
    ]

    public boolean detect() {
        String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)
        String tiffinfo = "tiffinfo $originalFilenameFullPath".execute().text
        //we have a TIFF, but what kind ? flat, pyramid, multi-page, ventana ?

        boolean notTiff = false
        excludeDescription.each {
            notTiff |= tiffinfo.contains(it)
        }
        if (notTiff) return false

        int nbTiffDirectory = StringUtils.countOccurrencesOf(tiffinfo, "TIFF Directory")

        return (nbTiffDirectory > 1)  //pyramid or multi-page, sufficient ?

    }
}
