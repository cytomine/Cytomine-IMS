package be.cytomine.formats.lightconvertable.geospatial

import be.cytomine.formats.detectors.TiffInfoDetector
import be.cytomine.formats.lightconvertable.VIPSConvertable
import grails.converters.JSON
import grails.util.Holders
import utils.MimeTypeUtils
import utils.ProcUtils

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
    }

//    @Override
//    def convert() {
//        // 1. Get bit depth
//        def gdalinfoExecutable = Holders.config.cytomine.gdalinfo
//        def gdalinfo = new ProcessBuilder("$gdalinfoExecutable", this.file.absolutePath).redirectErrorStream(true).start().text
//        def nbits
//        if (gdalinfo.contains("Int16"))
//            nbits = 16
//        else if (gdalinfo.contains("Int32") || gdalinfo.contains("Float32"))
//            nbits = 32
//        else
//            nbits = 8
//
//        // 2. Convert to 8/16/32 GeoTIFF
//        String source = this.file.absolutePath
//        String intermediate = [new File(this.file.absolutePath).getParent(), "_tmp.tif"].join(File.separator)
//        def gdaltranslateExecutable = Holders.config.cytomine.gdaltranslate
//        def convertCommand = """$gdaltranslateExecutable -co "NBITS=$nbits" -co "JPEG_QUALITY=100" -co "WEBP_LEVEL=100" "$source" "$intermediate" """
//
//        if (ProcUtils.executeOnShell(convertCommand) == 0) {
//            // 3. Convert to pyramidal tiff
//            String target = [new File(this.file.absolutePath).getParent(), UUID.randomUUID().toString() + ".tif"].join(File.separator)
//            def result = convertToPyramidalTIFF(intermediate, target)
//
//            File fileToDelete = new File(intermediate)
//            if(fileToDelete.exists()) {
//                fileToDelete.delete()
//            }
//
//            return result
//        }
//    }

    def properties() {
        def properties = super.properties()

        def gdalinfoExecutable = Holders.config.cytomine.gdalinfo
        def gdalinfo = JSON.parse(new ProcessBuilder("$gdalinfoExecutable", "-json", this.file.absolutePath).redirectErrorStream(true).start().text)

        flattenProperties(properties, "geotiff", "", gdalinfo)
    }

    def flattenProperties(properties, prefix, key, value) {
        key = (!key.isEmpty()) ? ".$key" : key
        if (value instanceof List) {
            value.eachWithIndex { it, i ->
                return flattenProperties(properties, "$prefix$key[$i]", "", it)
            }
        }
        else if (value instanceof Map) {
            value.each {
                return flattenProperties(properties, "$prefix$key", it.key, it.value)
            }
        }
        else {
           properties << [key: "$prefix$key", value: value]
        }

        return properties
    }
}
