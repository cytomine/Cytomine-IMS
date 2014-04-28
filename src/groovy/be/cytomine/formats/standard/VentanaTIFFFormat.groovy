package be.cytomine.formats.standard

import org.springframework.util.StringUtils

/**
 * Created by stevben on 28/04/14.
 */
class VentanaTIFFFormat extends TIFFFormat {

    private excludeDescription = [
            "Not a TIFF",
            "Make: Hamamatsu",
            "ImageDescription: Aperio Image Library"
    ]

    public boolean detect() {
        String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)
        String tiffinfo = "tiffinfo $originalFilenameFullPath".execute().text

        boolean notTiff = false
        excludeDescription.each {
            if (tiffinfo.contains(it)) notTiff |= true
        }
        if (notTiff) return false

        return (tiffinfo.contains("<iScan")) //ventana signature


    }
}
