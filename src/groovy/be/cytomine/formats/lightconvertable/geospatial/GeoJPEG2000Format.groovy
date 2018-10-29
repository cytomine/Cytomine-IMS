package be.cytomine.formats.lightconvertable.geospatial

import grails.converters.JSON
import grails.util.Holders
import utils.FilesUtils

class GeoJPEG2000Format extends GeoTIFFFormat {
    GeoJPEG2000Format() {
        mimeType = "image/jp2"
        extensions = ["jp2"]
    }

    boolean detect() {
        def gdalinfoExecutable = Holders.config.cytomine.gdalinfo
        def gdalinfo = JSON.parse(new ProcessBuilder("$gdalinfoExecutable", "-json", absoluteFilePath).redirectErrorStream(true).start().text)
        return FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase() == "jp2" &&
                !gdalinfo?.coordinateSystem?.wkt?.isEmpty()
    }
}
