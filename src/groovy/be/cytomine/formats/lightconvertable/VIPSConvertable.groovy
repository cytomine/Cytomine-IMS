package be.cytomine.formats.lightconvertable

import be.cytomine.formats.Format
import be.cytomine.formats.IConvertableImageFormat
import grails.util.Holders
import utils.FilesUtils
import utils.ProcUtils
import utils.ServerUtils

/**
 * Created by hoyoux on 25.09.15.
 */
abstract class VIPSConvertable extends Format implements IConvertableImageFormat {
    public String[] extensions = null
    public List<String> iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerCyto)

    @Override
    String[] convert() {
        String ext = FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase()
        String source = absoluteFilePath
        String target = [new File(absoluteFilePath).getParent(), UUID.randomUUID().toString() + ".tif"].join(File.separator)
        String intermediate = [new File(absoluteFilePath).getParent(), "_tmp.tif"].join(File.separator)

        println "ext : $ext"
        println "source : $source"
        println "target : $target"
        println "intermediate : $intermediate"

        //1. Look for vips executable
        def vipsExecutable = Holders.config.cytomine.vips

        //2. Pyramid command
        def pyramidCommand = """$vipsExecutable tiffsave "$source" "$target" --tile --pyramid --compression lzw --tile-width 256 --tile-height 256 --bigtiff"""

        boolean success = true

        success &= (ProcUtils.executeOnShell(pyramidCommand) == 0)

        if (success) {
            return [target]
        }
    }
}
