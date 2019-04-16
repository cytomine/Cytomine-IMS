package be.cytomine.formats.supported.digitalpathology


import be.cytomine.formats.CytomineFile
import be.cytomine.formats.MultipleFilesFormat
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

/**
 * Created by stevben on 22/04/14.
 */
class MiraxMRXSFormat extends OpenSlideFormat implements MultipleFilesFormat, OpenSlideDetector {

    String vendor = "mirax"

    // https://openslide.org/formats/mirax/
    // Associated labels: thumbnail, label, macro
    public MiraxMRXSFormat() {
        extensions = ["mrxs"]
        mimeType = MimeTypeUtils.MIMETYPE_MRXS
    }

    @Override
    boolean detect() {
        File mrxs = getRootFile(this.file)

        if(mrxs){
            this.file = new CytomineFile(mrxs.absolutePath)
            return OpenSlideDetector.super.detect()
        }
        return false
    }

    BufferedImage associated(String label) {
        BufferedImage bufferedImage = super.associated(label)
        return (label == "macro") ? ImageUtils.rotate90ToRight(bufferedImage) : bufferedImage
    }

    File getRootFile(File folder) {
        return folder.listFiles().find {file ->
            extensions.any {ext ->
                file.name.endsWith(".$ext")
            }
        }
    }
}