package be.cytomine.formats.digitalpathology

import be.cytomine.formats.standard.TIFFFormat
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
        String tiffinfo = "tiffinfo $uploadedFilePath".execute().text

        boolean notTiff = false
        excludeDescription.each {
            notTiff |= tiffinfo.contains(it)
        }
        if (notTiff) return false

        return (tiffinfo.contains("<iScan")) //ventana signature


    }

    public String convert() {
        boolean convertSuccessfull = true

        /*String convertFileName = new File(uploadedFilePath).getName()//uploadedFilePath.getStr("filename")
        convertFileName = convertFileName[0 .. (convertFileName.size() - uploadedFilePath.getStr("ext").size() - 2)]
        convertFileName = convertFileName + "_converted.tif"
        String originalFilenameFullPath = [ uploadedFilePath.getStr("path"), uploadedFilePath.getStr("filename")].join(File.separator)
        String convertedFilenameFullPath = [ uploadedFilePath.getStr("path"), convertFileName].join(File.separator)

        String biggestLayerFilename = uploadedFilePath.getStr("filename")
        biggestLayerFilename = biggestLayerFilename[0 .. (biggestLayerFilename.size() - uploadedFilePath.getStr("ext").size() - 2)]
        biggestLayerFilename = biggestLayerFilename + "_biggest_layer.tif"


        String biggestFilenameFullPath = [ uploadedFilePath.getStr("path"), biggestLayerFilename].join(File.separator)

        //1. Extract the biggest layer
        // vips im_vips2tiff 11GH076256_A2_CD3_100.tif:2 output_image.tif:deflate,,flat,,,,8
        def command = """vips im_vips2tiff $originalFilenameFullPath:2 $biggestFilenameFullPath:deflate,,flat,,,,8"""
        convertSuccessfull &= ProcUtils.executeOnShell(command) == 0

        //2. Pyramid
        // vips tiffsave output_image.tif output_image_compress.tif --tile --pyramid --compression jpeg --tile-width 256 --tile-height 256
        command = """vips tiffsave $biggestFilenameFullPath $convertedFilenameFullPath --tile --pyramid --compression jpeg --tile-width 256 --tile-height 256"""
        convertSuccessfull &= ProcUtils.executeOnShell(command)  == 0

        //3. Rm intermadiate file
        command = """rm $biggestFilenameFullPath"""
        convertSuccessfull &= ProcUtils.executeOnShell(command)  == 0

        if (convertSuccessfull) {
            return convertedFilenameFullPath
        }                                       */
    }
}
