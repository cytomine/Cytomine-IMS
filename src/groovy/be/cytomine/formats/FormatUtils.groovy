package be.cytomine.formats

import grails.util.Holders
import org.openslide.OpenSlide

class FormatUtils {

    static def getTiffInfo(def filePath) {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        return new ProcessBuilder("$tiffinfoExecutable", filePath).redirectErrorStream(true).start().text
    }

    static def getImageMagick(def filePath) {
        def identifyExecutable = Holders.config.cytomine.identify
        def command = ["$identifyExecutable", filePath]
        def proc = command.execute()
        proc.waitFor()
        return proc.in.text
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
