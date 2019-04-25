package be.cytomine.formats.supported.digitalpathology

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
import be.cytomine.formats.tools.MultipleFilesFormat
import be.cytomine.formats.tools.detectors.OpenSlideDetector
import groovy.util.logging.Log4j
import utils.MimeTypeUtils

@Log4j
class HamamatsuVMSFormat extends OpenSlideFormat implements MultipleFilesFormat, OpenSlideDetector {

    String vendor = "hamamatsu"

    // https://openslide.org/formats/hamamatsu/
    // Associated labels: macro
    HamamatsuVMSFormat() {
        super()
        extensions = ["vms"]
        mimeType = MimeTypeUtils.MIMETYPE_VMS
    }

    @Override
    boolean detect() {
        File vms = getRootFile(this.file)
        log.info vms

        if (vms) {
            this.file = new CytomineFile(vms.absolutePath)
            log.info this.file.openSlideVendor
            return OpenSlideDetector.super.detect()
        }
        return false
    }

    File getRootFile(File folder) {
        return folder.listFiles().find { file ->
            extensions.any { ext ->
                file.name.endsWith(".$ext")
            }
        }
    }
}
