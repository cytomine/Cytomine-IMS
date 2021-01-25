package be.cytomine.formats.tools.metadata

import be.cytomine.formats.tools.CytomineFile
import grails.converters.JSON
import groovy.util.logging.Log4j


@Log4j
class FfProbeMetadataExtractor extends MetadataExtractor {
    private CytomineFile file

    FfProbeMetadataExtractor(def file) {
        this.file = file
    }

    def properties() {
        def output = this.file.getFfProbeOutput()

        if (!output)
            return [:]

        def properties = flattenProperties([:], "Ffmpeg", "", JSON.parse(output))
        properties.remove("Ffmpeg.format.filename")
        return properties
    }
}
