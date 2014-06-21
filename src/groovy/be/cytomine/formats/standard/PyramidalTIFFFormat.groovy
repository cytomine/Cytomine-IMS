package be.cytomine.formats.standard

import be.cytomine.formats.digitalpathology.OpenSlideSingleFileFormat
import grails.util.Holders
import org.springframework.util.StringUtils
import java.awt.image.BufferedImage

/**
 * Created by stevben on 28/04/14.
 */
class PyramidalTIFFFormat  extends OpenSlideSingleFileFormat {

    public PyramidalTIFFFormat () {
        extensions = ["tif", "tiff"]
        mimeType = "image/tiff"
    }

    private excludeDescription = [
            "Not a TIFF",
            "<iScan",
            "Hamamatsu",
            "Aperio",
            "Leica"
    ]

    public boolean detect() {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        //we have a TIFF, but what kind ? flat, pyramid, multi-page, ventana ?

        boolean notTiff = false
        excludeDescription.each {
            notTiff |= tiffinfo.contains(it)
        }
        if (notTiff) return false

        int nbTiffDirectory = StringUtils.countOccurrencesOf(tiffinfo, "TIFF Directory")

        return (nbTiffDirectory > 1)  //pyramid or multi-page, sufficient ?

    }

    String convert() {
       return null //already a pyramid
    }



    public BufferedImage associated(String label) { //should be abstract
        if (label == "macro") {
            return thumb(256)
        }
    }

    BufferedImage thumb(int maxSize) {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        int numberOfTIFFDirectories = tiffinfo.count("TIFF Directory")
        getTIFFSubImage(numberOfTIFFDirectories - 1)
    }




}
