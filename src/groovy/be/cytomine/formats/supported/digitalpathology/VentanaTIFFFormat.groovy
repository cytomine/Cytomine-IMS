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

import be.cytomine.formats.tools.CustomExtensionFormat
import be.cytomine.formats.tools.detectors.OpenSlideDetector
import groovy.util.logging.Log4j
import org.codehaus.groovy.grails.web.util.TypeConvertingMap
import utils.ImageUtils
import utils.MimeTypeUtils

import java.awt.image.BufferedImage

@Log4j
class VentanaTIFFFormat extends OpenSlideFormat implements CustomExtensionFormat, OpenSlideDetector {

    String vendor = "ventana"
    String customExtension = "vtif"

    // https://openslide.org/formats/ventana/
    // Associated labels: macro, thumbnail
    VentanaTIFFFormat() {
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
