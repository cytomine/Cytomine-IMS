package be.cytomine.formats.supported.digitalpathology

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
class MiraxMRXSFormat extends OpenSlideMultipleFileFormat {

    public MiraxMRXSFormat() {
        extensions = ["mrxs"]
        vendor = "mirax"
        mimeType = "openslide/mrxs"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = "openslide.mpp-x"
        magnificiationProperty = "mirax.GENERAL.OBJECTIVE_MAGNIFICATION"
    }

    @Override
    boolean detect() {
        File uploadedFile = new File(absoluteFilePath);
        File mrxs = getRootFile(uploadedFile)

        if(mrxs){
            absoluteFilePath = mrxs.absolutePath
            return super.detect()
        }
        return false
    }

    BufferedImage associated(String label) {
        BufferedImage bufferedImage = super.associated(label)
        if (label == "macro"){
            return rotate90ToRight(bufferedImage)
        }
        else {
            return bufferedImage
        }
    }

    File getRootFile(File folder) {
        return folder.listFiles(). find { it.name.endsWith('.mrxs')}
    }
}