package be.cytomine.formats.lightconvertable.specialtiff

import be.cytomine.formats.ITIFFFormat

/*
 * Copyright (c) 2009-2017. Authors: see NOTICE file.
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

class CZITIFFFormat extends ConvertableTIFFFormat implements ITIFFFormat {


    public CZITIFFFormat () {
        extensions = ["tif", "tiff"]
    }

    public boolean detect() {
        String tiffinfo = getTiffInfo()
        return this.detect(tiffinfo)
    }

    boolean detect(String tiffinfo) {
        return tiffinfo.contains("ImageDescription: Label") && tiffinfo.contains("ImageDescription: SlidePreview")
    }
}
