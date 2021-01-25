package be.cytomine.formats.lightconvertable.specialtiff

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

import be.cytomine.formats.tools.detectors.TiffInfoDetector
import be.cytomine.formats.lightconvertable.VIPSConvertable
import groovy.util.logging.Log4j
import utils.MimeTypeUtils

@Log4j
class HuronTIFFFormat extends VIPSConvertable implements TiffInfoDetector {

    def requiredKeywords = [
            "Compression Scheme: None",
            "Photometric Interpretation: RGB color",
            "Source = Bright Field"
    ]

    def forbiddenKeywords = [
            "Compression Scheme: JPEG",
            "Photometric Interpretation: YCbCr"
    ]

    HuronTIFFFormat() {
        extensions = ["tif", "tiff"]
        mimeType = MimeTypeUtils.MIMETYPE_TIFF
    }
}
