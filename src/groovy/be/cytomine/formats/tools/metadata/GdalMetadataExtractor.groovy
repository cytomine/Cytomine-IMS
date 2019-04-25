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
import grails.converters.JSON
import grails.util.Holders
import groovy.util.logging.Log4j
import utils.ProcUtils

@Log4j
class GdalMetadataExtractor extends MetadataExtractor {
    private CytomineFile file

    GdalMetadataExtractor(def file) {
        this.file = file
    }

    def properties() {
        def command = Holders.config.cytomine.ims.detection.gdal.executable
        def exec = ProcUtils.executeOnShell("$command -json ${file.absolutePath}")

        if (exec.exit || !exec.out || exec.out.isEmpty())
            return [:]

        return flattenProperties([:], "GeoTiff", "", JSON.parse(exec.out))
    }

    def flattenProperties(def properties, String prefix, def key, def value) {
        key = (!key.isEmpty()) ? ".$key" : key
        if (value instanceof List) {
            value.eachWithIndex { it, i ->
                return flattenProperties(properties, "$prefix$key[$i]", "", it)
            }
        } else if (value instanceof Map) {
            value.each {
                return flattenProperties(properties, "$prefix$key", it.key, it.value)
            }
        } else {
            properties << [(prefix + key): value]
        }

        return properties
    }
}
