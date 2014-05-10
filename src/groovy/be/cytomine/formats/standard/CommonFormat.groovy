package be.cytomine.formats.standard

import be.cytomine.formats.ImageFormat
import utils.FilesUtils
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

    String convert(String workingPath) {
        String ext = FilesUtils.getExtensionFromFilename(uploadedFilePath).toLowerCase()
        String source = uploadedFilePath
        String target = [workingPath, new File(uploadedFilePath).getName()].join(File.separator).replace(".$ext", "_converted.tif")
        String intermediate = target.replace("_converted.tif","_tmp.tif")

        println "ext : $ext"
        println "source : $source"
        println "target : $target"
        println "intermediate : $intermediate"

        //1. Look for vips executable

        def executable = "`which vips`"

        def extractBandCommand = """$executable extract_band $source $intermediate[bigtiff,compression=lzw] 0 --n 3"""
        def rmIntermediatefile = """rm $intermediate"""
        def pyramidCommand = """$executable tiffsave "$intermediate" "$target" --tile --pyramid --compression lzw --tile-width 256 --tile-height 256 --bigtiff"""

        boolean success = true

        success &= (ProcUtils.executeOnShell(extractBandCommand) == 0)

        if(!success) {
            success = true
            extractBandCommand = """$executable extract_band $source $intermediate[bigtiff,compression=lzw] 0 --n 1"""
            success &= (ProcUtils.executeOnShell(extractBandCommand) == 0)
        }

        success &= (ProcUtils.executeOnShell(pyramidCommand) == 0)
        success &= (ProcUtils.executeOnShell(rmIntermediatefile) == 0)

        if (success) {
            return target
        }
    }
}
