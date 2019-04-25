package be.cytomine.formats.metadata

import be.cytomine.formats.CytomineFile
import be.cytomine.formats.detectors.GdalDetector
import grails.util.Holders
import groovy.json.JsonSlurper
import utils.ProcUtils

class GdalMetadataExtractor extends MetadataExtractor {
    private CytomineFile file

    public GdalMetadataExtractor(def file) {
        this.file = file
    }

    def properties() {
        def command = Holders.config.cytomine.ims.detection.gdal.executable
        def exec = ProcUtils.executeOnShell("$command -json ${file.absolutePath}")

        if (exec.exit || !exec.out || exec.out.isEmpty())
            return [:]

        return flattenProperties(properties, "GeoTiff", "", exec.out)

    }

    def flattenProperties(def properties, String prefix, def key, def value) {
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
            properties << [(prefix + key): value]
        }

        return properties
    }
}
