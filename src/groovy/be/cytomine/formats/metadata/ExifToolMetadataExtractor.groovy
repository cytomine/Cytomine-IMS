package be.cytomine.formats.metadata

import be.cytomine.formats.CytomineFile
import grails.converters.JSON
import grails.util.Holders
import groovy.json.JsonSlurper
import utils.ProcUtils

class ExifToolMetadataExtractor extends MetadataExtractor {

    private CytomineFile file

    def propertiesToRemove = [
            "SourceFile",
            "ExifTool:ExifToolVersion",
            "File:FileName",
            "File:Directory",
            "File:FileSize",
            "File:FileModifyDate",
            "File:FileAccessDate",
            "File:FileInodeChangeDate",
            "File:FilePermissions",
            "File:FileType",
            "File:FileTypeExtension",
            "File:MIMEType" // ?
    ]

    public ExifToolMetadataExtractor(def file) {
        this(file, [])
    }

    public ExifToolMetadataExtractor(def file, def toRemove) {
        this.file = file
        this.propertiesToRemove += toRemove
    }


    def properties() {
        def command = Holders.config.cytomine.ims.metadata.exiftool.executable
        def exec = ProcUtils.executeOnShell("$command -All -s -G -j -u -e ${file.absolutePath}")

        if (exec.exit || !exec.out || exec.out.isEmpty())
            return [:]


        def exifProperties = new JsonSlurper().parseText(exec.out[1..-1]).findAll {
            it.value != null &&
                    !(it.value as String).isEmpty() &&
                    !(it.value as String).contains("use -b option to extract") &&
                    !(it.key in propertiesToRemove)
        }

        return exifProperties.collectEntries {
            [(renameKey(it.key)): it.value]
        }
    }

    String renameKey(String key) {
        key.replaceAll(":", ".")
    }

}
