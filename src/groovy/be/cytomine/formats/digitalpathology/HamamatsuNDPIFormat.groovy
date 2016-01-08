package be.cytomine.formats.digitalpathology

/*
 * Copyright (c) 2009-2016. Authors: see NOTICE file.
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

import org.openslide.OpenSlide
import utils.FilesUtils

/**
 * Created by stevben on 22/04/14.
 */
class HamamatsuNDPIFormat extends OpenSlideSingleFileFormat {

    public HamamatsuNDPIFormat() {
        extensions = ["ndpi"]
        vendor = "hamamatsu"
        mimeType = "openslide/ndpi"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = "openslide.mpp-x"
        magnificiationProperty = "hamamatsu.SourceLens"
    }

    boolean detect() {
        if (!super.detect()) return false //not an hamamatsu format
        if(FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase().equals("tif")) return false //hack: if convert ndpi to tif => still hamamatsu metadata
        return !new OpenSlide(new File(absoluteFilePath)).properties.keySet().contains("hamamatsu.MapFile")



    }
}
