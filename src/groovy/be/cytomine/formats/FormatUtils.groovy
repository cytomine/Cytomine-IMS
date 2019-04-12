package be.cytomine.formats

import grails.util.Holders
import org.openslide.OpenSlide
import utils.ProcUtils

class FormatUtils {

    static def getTiffInfo(def filePath) {
        def tiffinfoExecutable = Holders.config.cytomine.ims.detection.tiffinfo.executable
        def command = """$tiffinfoExecutable $filePath """
        return new ProcessBuilder("$tiffinfoExecutable", filePath).redirectErrorStream(true).start().text
    }

    static def getImageMagick(def filePath) {
        def identifyExecutable = Holders.config.cytomine.ims.detection.identify.executable
        def command = """$identifyExecutable $filePath """
        return ProcUtils.executeOnShell(command).out
    }

    static def getOpenSlideVendor(def file) {
        if (!file.canRead()) {
            return false
        }

        try {
            return OpenSlide.detectVendor(file)
        } catch (IOException ignored) {
            //Not a file that OpenSlide can recognize
            return false
        }

    }
}
