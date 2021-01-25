package be.cytomine.formats.tools.metadata

/*
 * Copyright (c) 2009-2019. Authors: see NOTICE file.
 *
 * Licensed under the GNU Lesser General Public License, Version 2.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import be.cytomine.formats.tools.CytomineFile
import grails.util.Holders
import groovy.json.JsonSlurper
import groovy.util.logging.Log4j
import utils.ProcUtils

@Log4j
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

    ExifToolMetadataExtractor(def file) {
        this(file, [])
    }

    ExifToolMetadataExtractor(def file, def toRemove) {
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
                    !(it.value as String).replaceAll("\\\\u[0-9A-Fa-f]{4}", "").isEmpty() &&
                    !(it.value as String).replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").isEmpty() &&
                    !(it.value as String).contains("use -b option to extract") &&
                    !(it.key in propertiesToRemove)
        }

        return exifProperties.collectEntries {
            [(renameKey(it.key)):
                     (it.value as String)
                             .replaceAll("\\\\u[0-9A-Fa-f]{4}", "")
                             .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
            ]
        }
    }

    String renameKey(String key) {
        key.replaceAll(":", ".")
    }

}
