package be.cytomine.formats.digitalpathology

import be.cytomine.formats.standard.TIFFFormat
import grails.util.Holders
import utils.ProcUtils

/**
 * Created by stevben on 28/04/14.
 */
class VentanaTIFFFormat extends TIFFFormat {

    private excludeDescription = [
            "Not a TIFF",
            "Make: Hamamatsu",
            "Leica",
            "ImageDescription: Aperio Image Library"
    ]

    public boolean detect() {
        def tiffinfoExecutable = Holders.config.grails.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text

        boolean notTiff = false
        excludeDescription.each {
            notTiff |= tiffinfo.contains(it)
        }
        if (notTiff) return false

        return (tiffinfo.contains("<iScan")) //ventana signature


    }

    String convert(String workingPath) {
        boolean convertSuccessfull = true

        String source = absoluteFilePath
        String target = [new File(absoluteFilePath).getParent(), "_converted.tif"].join(File.separator)
        String intermediate = [new File(absoluteFilePath).getParent(), "_tmp.tif"].join(File.separator)

        def vipsExecutable = Holders.config.grails.vips

        //1. Extract the biggest layer
        // vips im_vips2tiff 11GH076256_A2_CD3_100.tif:2 output_image.tif:deflate,,flat,,,,8
        def command = """$vipsExecutable im_vips2tiff $source:2 $intermediate:deflate,,flat,,,,8"""
        convertSuccessfull &= ProcUtils.executeOnShell(command) == 0

        //2. Pyramid
        // vips tiffsave output_image.tif output_image_compress.tif --tile --pyramid --compression jpeg --tile-width 256 --tile-height 256
        command = """$vipsExecutable tiffsave $intermediate $target --tile --pyramid --compression jpeg --tile-width 256 --tile-height 256"""
        convertSuccessfull &= ProcUtils.executeOnShell(command)  == 0

        //3. Rm intermediate file
        command = """rm $intermediate"""
        convertSuccessfull &= ProcUtils.executeOnShell(command)  == 0

        if (convertSuccessfull) {
            return target
        }
    }
}
