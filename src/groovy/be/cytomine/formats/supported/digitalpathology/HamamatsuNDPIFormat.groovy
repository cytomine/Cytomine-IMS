package be.cytomine.formats.supported.digitalpathology

import be.cytomine.formats.detectors.OpenSlideDetector
import org.openslide.OpenSlide
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
/**
 * Created by stevben on 22/04/14.
 */
class HamamatsuNDPIFormat extends OpenSlideFormat implements OpenSlideDetector {

    String vendor = "hamamatsu"

    HamamatsuNDPIFormat() {
        extensions = ["ndpi"]
        mimeType = MimeTypeUtils.MIMETYPE_NDPI

        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = "openslide.mpp-x"
        magnificationProperty = "hamamatsu.SourceLens"
    }

    boolean detect() {
        if (!OpenSlideDetector.super.detect()) return false //not an hamamatsu format
        if (file.extension() == "tif") return false //hack: if convert ndpi to tif => still hamamatsu metadata
        return !new OpenSlide(file).properties.keySet().contains("hamamatsu.MapFile")
    }
}
