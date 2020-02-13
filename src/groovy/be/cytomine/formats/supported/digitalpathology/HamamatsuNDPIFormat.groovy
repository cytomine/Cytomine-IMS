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

import be.cytomine.formats.tools.detectors.OpenSlideDetector
import groovy.util.logging.Log4j
import org.openslide.OpenSlide
import utils.MimeTypeUtils

@Log4j
class HamamatsuNDPIFormat extends OpenSlideFormat implements OpenSlideDetector {

    String vendor = "hamamatsu"
    String customExtension = "ndpi"

    // https://openslide.org/formats/hamamatsu/
    // Associated labels: macro
    HamamatsuNDPIFormat() {
        super()
        extensions = ["ndpi"]
        mimeType = MimeTypeUtils.MIMETYPE_NDPI
    }

    boolean detect() {
        if (!OpenSlideDetector.super.detect()) return false //not an hamamatsu format
        if (file.extension() == "tif") return false //hack: if convert ndpi to tif => still hamamatsu metadata
        return !new OpenSlide(file).properties.keySet().contains("hamamatsu.MapFile")
    }
}
