package be.cytomine.formats.lightconvertable.geospatial

import be.cytomine.formats.detectors.TiffInfoDetector
import be.cytomine.formats.lightconvertable.VIPSConvertable
import be.cytomine.formats.metadata.GdalMetadataExtractor
import grails.converters.JSON
import grails.util.Holders
import utils.MimeTypeUtils
import utils.ProcUtils
import utils.PropertyUtils

class GeoTIFFFormat extends VIPSConvertable implements TiffInfoDetector {

    // http://web.archive.org/web/20160731005338/http://www.remotesensing.org:80/geotiff/spec/geotiff6.html#6.1
    def possibleKeywords = [
            "Tag 33550:",
            "Tag 34264:",
            "Tag 33922:",
            "Tag 34735:",
            "Tag 34736:",
            "Tag 34737:"
    ]

    GeoTIFFFormat() {
        extensions = ["tif", "tiff"]
        mimeType = MimeTypeUtils.MIMETYPE_TIFF

        // https://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/EXIF.html
        cytominePropertyKeys[PropertyUtils.CYTO_WIDTH] = "EXIF.ImageWidth"
        cytominePropertyKeys[PropertyUtils.CYTO_HEIGHT] = "EXIF.ImageHeight"
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES] = "EXIF.XResolution"
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES] = "EXIF.YResolution"
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES_UNIT] = "EXIF.ResolutionUnit"
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES_UNIT] = "EXIF.ResolutionUnit"
        cytominePropertyKeys[PropertyUtils.CYTO_BPS] = "EXIF.BitsPerSample"
        cytominePropertyKeys[PropertyUtils.CYTO_SPP] = "EXIF.SamplesPerPixel"
        cytominePropertyKeys[PropertyUtils.CYTO_COLORSPACE] = "EXIF.PhotometricInterpretation"
        cytominePropertyParsers[PropertyUtils.CYTO_BPS] = PropertyUtils.parseIntFirstWord
    }

    def properties() {
        def properties = super.properties()
        properties += new GdalMetadataExtractor(this.file).properties()

        return properties
    }
}
