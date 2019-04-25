package be.cytomine.formats.lightconvertable

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

import be.cytomine.formats.tools.detectors.ImageMagickDetector
import groovy.util.logging.Log4j
import utils.MimeTypeUtils
import utils.PropertyUtils

@Log4j
class PGMFormat extends CommonFormat implements ImageMagickDetector {

    String IMAGE_MAGICK_FORMAT_IDENTIFIER = "PGM"

    // http://netpbm.sourceforge.net/doc/pgm.html
    PGMFormat() {
        extensions = ["pgm"]
        mimeType = MimeTypeUtils.MIMETYPE_PPM

        cytominePropertyKeys[PropertyUtils.CYTO_WIDTH] = "File.ImageWidth"
        cytominePropertyKeys[PropertyUtils.CYTO_HEIGHT] = "File.ImageHeight"
        cytominePropertyKeys[PropertyUtils.CYTO_BPS] = "File.MaxVal"
        cytominePropertyParsers[PropertyUtils.CYTO_BPS] = { x ->
            return (PropertyUtils.parseInt(x) > 256) ? 16 : 8
        }
    }

    def cytomineProperties() {
        def properties = super.cytomineProperties()
        properties[PropertyUtils.CYTO_SPP] = 1 //PGM = Portable Gray Map
        return properties
    }
}
