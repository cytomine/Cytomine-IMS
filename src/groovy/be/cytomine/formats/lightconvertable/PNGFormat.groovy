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
class PNGFormat extends CommonFormat implements ImageMagickDetector {

    String IMAGE_MAGICK_FORMAT_IDENTIFIER = "PNG"

    PNGFormat() {
        extensions = ["png"]
        mimeType = MimeTypeUtils.MIMETYPE_PNG

        // https://sno.phy.queensu.ca/~phil/exiftool/TagNames/PNG.html
        cytominePropertyKeys[PropertyUtils.CYTO_WIDTH] = "PNG.ImageWidth"
        cytominePropertyKeys[PropertyUtils.CYTO_HEIGHT] = "PNG.ImageHeight"
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES] = "PNG.PixelsPerUnitX"
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES] = "PNG.PixelsPerUnitY"
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES_UNIT] = "PNG.PixelUnits" // Unknown or meters
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES_UNIT] = "PNG.PixelUnits" // Unknown or meters
        cytominePropertyKeys[PropertyUtils.CYTO_BPS] = "PNG.BitDepth"
        cytominePropertyKeys[PropertyUtils.CYTO_SPP] = "PNG.ColorType"
        cytominePropertyKeys[PropertyUtils.CYTO_COLORSPACE] = "PNG.ColorType"

        cytominePropertyParsers[PropertyUtils.CYTO_SPP] = { x ->
            x = x.toLowerCase()
            if (x == "grayscale") return 1
            else if (x == "rgb") return 3
            else if (x == "grayscale with alpha") return 2
            else if (x == "rgb with alpha") return 4
            else return null
        }

        // We have to reverse X & Y resolution as they are given in pixel per unit instead of unit per pixel
        def reverseDouble = { x ->
            def n = PropertyUtils.parseDouble(x)
            if (!n) return null
            return 1.0 / n
        }

        cytominePropertyParsers[PropertyUtils.CYTO_X_RES] = reverseDouble
        cytominePropertyParsers[PropertyUtils.CYTO_Y_RES] = reverseDouble
    }
}
