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
class JPEGFormat extends CommonFormat implements ImageMagickDetector {

    String IMAGE_MAGICK_FORMAT_IDENTIFIER = "JPEG"

    JPEGFormat() {
        extensions = ["jpg", "jpeg"]
        mimeType = MimeTypeUtils.MIMETYPE_JPEG

        cytominePropertyKeys[PropertyUtils.CYTO_WIDTH] = "File.ImageWidth"
        cytominePropertyKeys[PropertyUtils.CYTO_HEIGHT] = "File.ImageHeight"
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES] = "JFIF.XResolution" // to check
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES] = "JFIF.YResolution" // to check
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES_UNIT] = "JFIF.ResolutionUnit" // to check
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES_UNIT] = "JFIF.ResolutionUnit" // to check
        cytominePropertyKeys[PropertyUtils.CYTO_BPS] = "File.BitsPerSample"
        cytominePropertyKeys[PropertyUtils.CYTO_SPP] = "File.ColorComponents"
        cytominePropertyKeys[PropertyUtils.CYTO_COLORSPACE] = "File.ColorSpace" //to check
        cytominePropertyParsers[PropertyUtils.CYTO_BPS] = PropertyUtils.parseIntFirstWord
    }
}
