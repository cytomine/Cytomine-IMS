package be.cytomine.formats.supported.digitalpathology

import be.cytomine.formats.CustomExtensionFormat
import be.cytomine.formats.detectors.OpenSlideDetector
import utils.ImageUtils
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

import java.awt.image.BufferedImage
import grails.util.Holders
import utils.ServerUtils

class VentanaTIFFFormat extends OpenSlideFormat implements CustomExtensionFormat, OpenSlideDetector {

    String vendor = "ventana"
    String customExtension = "vtif"

    // https://openslide.org/formats/ventana/
    // Associated labels: macro, thumbnail
    public VentanaTIFFFormat() {
        super()
        extensions = ["tif", customExtension]
        mimeType = MimeTypeUtils.MIMETYPE_VTIFF
    }

    boolean detect() {
        return OpenSlideDetector.super.detect() && extensions.contains(file.extension())
    }

    BufferedImage associated(String label) {
        BufferedImage bufferedImage = super.associated(label)
        return (label == "macro") ? ImageUtils.rotate90ToRight(bufferedImage) : bufferedImage
    }
}
