package be.cytomine.formats.standard

import be.cytomine.formats.ImageFormat
import utils.ProcUtils

/**
 * Created by stevben on 22/04/14.
 */
abstract class CommonFormat extends ImageFormat {

    public IMAGE_MAGICK_FORMAT_IDENTIFIER = null

    public boolean detect() {
        String command = "identify -verbose $uploadedFilePath"
        def proc = command.execute()
        proc.waitFor()
        String stdout = proc.in.text
        return stdout.contains(IMAGE_MAGICK_FORMAT_IDENTIFIER)
    }

    String convert() {
        /*String convertFileName = uploadedFilePath.getStr("filename")
        convertFileName = convertFileName[0 .. (convertFileName.size() - uploadedFilePath.getStr("ext").size() - 2)]
        convertFileName = convertFileName + "_converted.tif"
        String originalFilenameFullPath = [ uploadedFilePath.getStr("path"), uploadedFilePath.getStr("filename")].join(File.separator)
        String convertedFilenameFullPath = [ uploadedFilePath.getStr("path"), convertFileName].join(File.separator)

        //1. Look for vips executable

        def executable = "/usr/local/bin/vips"
        if (System.getProperty("os.name").contains("OS X")) {
            executable = "/usr/local/bin/vips"
        }
        def intermediateFile = convertedFilenameFullPath.replace(".tif",".tmp.tif")


        def extractBandCommand = """$executable extract_band $originalFilenameFullPath $intermediateFile[bigtiff,compression=lzw] 0 --n 3"""
        def rmIntermediatefile = """rm $intermediateFile"""
        def pyramidCommand = """$executable tiffsave "$intermediateFile" "$convertedFilenameFullPath" --tile --pyramid --compression lzw --tile-width 256 --tile-height 256 --bigtiff"""

        boolean success = true

        success &= (ProcUtils.executeOnShell(extractBandCommand) == 0)

        if(!success) {
            success = true
            extractBandCommand = """$executable extract_band $originalFilenameFullPath $intermediateFile[bigtiff,compression=lzw] 0 --n 1"""
            success &= (ProcUtils.executeOnShell(extractBandCommand) == 0)
        }

        success &= (ProcUtils.executeOnShell(pyramidCommand) == 0)
        success &= (ProcUtils.executeOnShell(rmIntermediatefile) == 0)

        if (success) {
            return convertedFilenameFullPath
        }    */
    }
}
