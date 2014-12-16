package be.cytomine.formats.standard

import be.cytomine.formats.digitalpathology.OpenSlideSingleFileFormat
import grails.util.Holders
import org.springframework.util.StringUtils
import utils.FilesUtils
import utils.ServerUtils

import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Created by stevben on 28/04/14.
 */
class PyramidalTIFFFormat  extends OpenSlideSingleFileFormat {

    public PyramidalTIFFFormat () {
        extensions = ["tif", "tiff"]
        mimeType = "image/pyrtiff"
    }

    private excludeDescription = [
            "Not a TIFF",
            "<iScan",
            //"Hamamatsu",
            "Aperio",
            "Leica",
            "PHILIPS",
            "OME-XML"
    ]

    public boolean detect() {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        //we have a TIFF, but what kind ? flat, pyramid, multi-page, ventana ?

        boolean notTiff = false
        excludeDescription.each {
            notTiff |= tiffinfo.contains(it)
        }
        println "${tiffinfo.contains("Hamamatsu")}"
        println "${!FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase().equals("tif")}"
        println "notTiff=${notTiff}"
        if(tiffinfo.contains("Hamamatsu") && !FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase().equals("tif")) {
            return false //otherwise its a tiff file converted from ndpi
        }
        if (notTiff) return false

        int nbTiffDirectory = StringUtils.countOccurrencesOf(tiffinfo, "TIFF Directory")

        //pyramid or multi-page, sufficient ?
        if (nbTiffDirectory > 1) return true
        else if (nbTiffDirectory == 1) { //check if very small tiff
            //get width & height from tiffinfo...
            int maxWidth = 0
            int maxHeight = 0
            tiffinfo.tokenize( '\n' ).findAll {
                it.contains 'Image Width:'
            }.each {
                def tokens = it.tokenize(" ")
                int width = Integer.parseInt(tokens.get(2))
                int height = Integer.parseInt(tokens.get(5))
                maxWidth = Math.max(maxWidth, width)
                maxHeight = Math.max(maxHeight, height)
            }

            return (maxWidth < 256 && maxHeight < 256)
        }


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
        String thumbURL = "${ServerUtils.getServer(iipURL)}?fif=$absoluteFilePath&SDS=0,90&CNT=1.0&CVT=jpeg&QLT=99"
        println thumbURL
		return ImageIO.read(new URL(thumbURL))
    }




}
