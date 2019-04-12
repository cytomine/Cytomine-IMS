package be.cytomine.formats.lightconvertable.specialtiff


import be.cytomine.formats.detectors.TiffInfoDetector
import be.cytomine.formats.lightconvertable.VIPSConvertable

/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
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

import org.springframework.util.StringUtils
import utils.MimeTypeUtils


class PlanarTIFFFormat extends VIPSConvertable implements TiffInfoDetector {

    def forbiddenKeywords = [
            "Not a TIFF",
            "<iScan",
            "Make: Hamamatsu",
            "Leica",
            "ImageDescription: Aperio Image Library",
            "PHILIPS"
    ]

    PlanarTIFFFormat() {
        extensions = ["tif", "tiff"]
        mimeType = MimeTypeUtils.MIMETYPE_TIFF
    }

    public boolean detect() {
        boolean detected = TiffInfoDetector.super.detect()
        if (!detected) return false

        //single layer tiff, we ne need to create a pyramid version
        int nbTiffDirectory = StringUtils.countOccurrencesOf(file.getTiffInfoOutput(), "TIFF Directory")
        return (nbTiffDirectory == 1 && !file.getTiffInfoOutput().contains("Tile"))
    }
}
