package be.cytomine.image

import be.cytomine.formats.tools.CytomineFile

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

import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.supported.NativeFormat

class TileService {

    def getTileUrlZoomify(def params) {
        String fif = params.zoomify
        /*remove the "/" at the end of the path injected by openlayers (OL2).
          I Did not find the way to avoid it from OL2 (BS)
         */
        if (fif.endsWith("/"))
            fif = fif.substring(0, fif.length()-1)
        String mimeType = params.mimeType
        NativeFormat imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType, true)
        return imageFormat.tileURL(params)
    }

    def getTileUrlIIP(def params) {
        NativeFormat imageFormat = new FormatIdentifier(new CytomineFile(fif)).identify(mimeType, true)
        return imageFormat.tileURL(params)
    }
}