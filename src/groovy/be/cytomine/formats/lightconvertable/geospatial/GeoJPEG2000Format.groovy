package be.cytomine.formats.lightconvertable.geospatial

import be.cytomine.exception.ConversionException
import be.cytomine.formats.CytomineFile
import be.cytomine.formats.NotNativeFormat
import be.cytomine.formats.detectors.GdalDetector
import be.cytomine.formats.metadata.GdalMetadataExtractor
import grails.util.Holders
import utils.FilesUtils
import utils.MimeTypeUtils
import utils.ProcUtils
import utils.PropertyUtils

class GeoJPEG2000Format extends NotNativeFormat implements GdalDetector {
    GeoJPEG2000Format() {
        extensions = ["jp2"]
        mimeType = MimeTypeUtils.MIMETYPE_JP2

        // https://sno.phy.queensu.ca/~phil/exiftool/TagNames/Jpeg2000.html
        cytominePropertyKeys[PropertyUtils.CYTO_WIDTH] = "Jpeg2000.ImageWidth"
        cytominePropertyKeys[PropertyUtils.CYTO_HEIGHT] = "Jpeg2000.ImageHeight"
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES] = "Jpeg2000.DisplayXResolution" // to check
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES] = "Jpeg2000.DisplayYResolution" // to check
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES_UNIT] = "Jpeg2000.DisplayXResolutionUnit" // to check
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES_UNIT] = "Jpeg2000.DisplayYResolutionUnit" // to check
        cytominePropertyKeys[PropertyUtils.CYTO_BPS] = "Jpeg2000.BitsPerComponent"
        cytominePropertyKeys[PropertyUtils.CYTO_SPP] = "Jpeg2000.NumberOfComponents"
        cytominePropertyKeys[PropertyUtils.CYTO_COLORSPACE] = "Jpeg2000.ColorSpace"
        cytominePropertyParsers[PropertyUtils.CYTO_BPS] = PropertyUtils.parseIntFirstWord
    }

    boolean detect() {
        return this.file.extension() in extensions && GdalDetector.super.detect()
    }

    @Override
    def convert() {
        String targetName = (this.file.name - ".${this.file.extension()}") + "_geo.tif"
        CytomineFile target = new CytomineFile(this.file.parent, FilesUtils.correctFilename(targetName), this.file.c, this.file.z, this.file.t)

        def gdalinfo = this.file.getGdalInfoOutput()
        def nbits = (gdalinfo.contains("Int16")) ? 16 : ((gdalinfo.contains("Int32") || gdalinfo.contains("Float32")) ? 32 : 8)

        def gdal = Holders.config.cytomine.ims.conversion.gdal.executable
        def convertCommand = """$gdal -co "NBITS=$nbits" -co "JPEG_QUALITY=100" -co "WEBP_LEVEL=100" $file.absolutePath $target.absolutePath """
        if (ProcUtils.executeOnShell(convertCommand).exit != 0 || !target.exists()) {
            throw new ConversionException("${file.absolutePath} hasn't been converted to ${target.absolutePath}")
        }

        return [target]
    }

    def properties() {
        def properties = super.properties()
        properties += new GdalMetadataExtractor(this.file).properties()

        return properties
    }
}
