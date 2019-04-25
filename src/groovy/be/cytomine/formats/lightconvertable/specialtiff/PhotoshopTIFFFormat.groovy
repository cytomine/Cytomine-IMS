package be.cytomine.formats.lightconvertable.specialtiff


import be.cytomine.formats.detectors.TiffInfoDetector
import be.cytomine.formats.lightconvertable.VIPSConvertable
import utils.MimeTypeUtils
import utils.PropertyUtils

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


/**
 * Created by hoyoux on 16.02.15.
 */
class PhotoshopTIFFFormat extends VIPSConvertable implements TiffInfoDetector {

    def requiredKeywords = [
            "Software: Adobe Photoshop"
    ]

    PhotoshopTIFFFormat() {
        extensions = ["tif", "tiff"]
        mimeType = MimeTypeUtils.MIMETYPE_TIFF

        // https://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/EXIF.html
        //TODO: https://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/Photoshop.html
        cytominePropertyKeys[PropertyUtils.CYTO_WIDTH] = "EXIF.ImageWidth"
        cytominePropertyKeys[PropertyUtils.CYTO_HEIGHT] = "EXIF.ImageHeight"
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES] = "EXIF.XResolution"
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES] = "EXIF.YResolution"
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES_UNIT] = "EXIF.ResolutionUnit"
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES_UNIT] = "EXIF.ResolutionUnit"
        cytominePropertyKeys[PropertyUtils.CYTO_BPS] = "EXIF.BitsPerSample"
        cytominePropertyKeys[PropertyUtils.CYTO_SPP] = "EXIF.SamplesPerPixel"
        cytominePropertyKeys[PropertyUtils.CYTO_COLORSPACE] = "EXIF.PhotometricInterpretation"
        cytominePropertyParsers[PropertyUtils.CYTO_BPS] = PropertyUtils.parseIntFirstWord
    }
}
