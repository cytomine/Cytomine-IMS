package be.cytomine.formats.lightconvertable.geospatial

import be.cytomine.exception.ConversionException
import be.cytomine.formats.CytomineFile
import grails.converters.JSON
import grails.util.Holders
import utils.FilesUtils
import utils.MimeTypeUtils
import utils.ProcUtils

class GeoJPEG2000Format extends GeoTIFFFormat {
    GeoJPEG2000Format() {
        extensions = ["jp2"]
        mimeType = MimeTypeUtils.MIMETYPE_JP2
    }

    boolean detect() {
        def gdalinfoExecutable = Holders.config.cytomine.gdalinfo
        def gdalinfo = JSON.parse(new ProcessBuilder("$gdalinfoExecutable", "-json", this.file.absolutePath).redirectErrorStream(true).start().text)
        return this.file.extension() == "jp2" &&
                !gdalinfo?.coordinateSystem?.wkt?.isEmpty()
    }

    @Override
    def convert() {
        String targetName = (this.file.name - ".${this.file.extension()}") + "_geo.tif"
        CytomineFile target = new CytomineFile(this.file.parent, FilesUtils.correctFilename(targetName), this.file.c, this.file.z, this.file.t)

        def gdalinfoExecutable = Holders.config.cytomine.gdalinfo
        def gdalinfo = new ProcessBuilder("$gdalinfoExecutable", this.file.absolutePath).redirectErrorStream(true).start().text
        def nbits
        if (gdalinfo.contains("Int16"))
            nbits = 16
        else if (gdalinfo.contains("Int32") || gdalinfo.contains("Float32"))
            nbits = 32
        else
            nbits = 8

        def gdal = Holders.config.cytomine.ims.conversion.gdal.executable
        def convertCommand = """$gdal -co "NBITS=$nbits" -co "JPEG_QUALITY=100" -co "WEBP_LEVEL=100" $file.absolutePath $target.absolutePath """
        if (ProcUtils.executeOnShell(convertCommand).exit != 0 || !target.exists()) {
            throw new ConversionException("${file.absolutePath} hasn't been converted to ${target.absolutePath}")
        }

        return [target]
    }
}
