package be.cytomine.formats.lightconvertable

import be.cytomine.exception.ConversionException
import be.cytomine.formats.CytomineFile
import be.cytomine.formats.Format
import be.cytomine.formats.IConvertableImageFormat
import be.cytomine.formats.NotNativeFormat
import grails.util.Holders
import utils.FilesUtils
import utils.ProcUtils
import utils.ServerUtils

/**
 * Created by hoyoux on 25.09.15.
 */
abstract class VIPSConvertable extends NotNativeFormat {

    @Override
    def convert() {
        String targetName = (this.file.name - ".${this.file.extension()}") + "_pyr.tif"
        CytomineFile target = new CytomineFile(this.file.parent, FilesUtils.correctFilename(targetName), this.file.c, this.file.z, this.file.t)

        return [convertToPyramidalTIFF(file, target)]
    }

    static def convertToPyramidalTIFF(CytomineFile source, CytomineFile target) {

        def vipsExecutable = Holders.config.cytomine.ims.conversion.vips.executable
        def compression = Holders.config.cytomine.ims.conversion.vips.compression ?: "jpeg -Q 95"
        def tileSize = 256

        def command = """$vipsExecutable tiffsave $source.absolutePath $target.absolutePath --bigtiff --tile --tile-width $tileSize --tile-height $tileSize --pyramid --compression $compression"""

        if (ProcUtils.executeOnShell(command).exit != 0 || !target.exists())
            throw new ConversionException("${source.absolutePath} hasn't been converted to ${target.absolutePath}")

        return target
    }
}
