package be.cytomine.formats.heavyconvertable


import be.cytomine.formats.detectors.TiffInfoDetector
import utils.MimeTypeUtils

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

class OMETIFFFormat extends BioFormatConvertable implements TiffInfoDetector {

    def possibleKeywords = [
            "OME-TIFF"
    ]

    boolean group = true

    OMETIFFFormat() {
        extensions = ["ome.tiff"]
        mimeType = MimeTypeUtils.MIMETYPE_OMETIFF
    }


    boolean detect() {
        if (TiffInfoDetector.super.detect())
            return true

        String tiffinfo = file.getTiffInfoOutput()
        if (tiffinfo.contains("hyperstack=true")
                && Integer.parseInt(tiffinfo.split("\n").find { it.contains("images=") }.split("=")[1]) > 1
                && !tiffinfo.contains("Tile Width")) {
            return true
        }
    }

    boolean getGroup() {
        return true
    }

    @Override
    boolean getOnlyBiggestSerie() {
        return false
    }
}
