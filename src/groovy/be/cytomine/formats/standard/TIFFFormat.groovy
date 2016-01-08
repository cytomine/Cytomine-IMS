package be.cytomine.formats.standard

import grails.util.Holders
import utils.ServerUtils

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
/**
 * Created by stevben on 22/04/14.
 */
abstract class TIFFFormat extends CommonFormat {

    public TIFFFormat() {
        extensions = ["tif", "tiff"]
        mimeType = "image/tiff"
        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerBase)
    }

    def properties() {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        def properties = [[key : "mimeType", value : mimeType]]
        int maxWidth = 0
        int maxHeight = 0
        tiffinfo.tokenize( '\n' ).findAll {
            it.contains 'Image Width:'
        }.each {
            def tokens = it.tokenize(" ")
            int width = Integer.parseInt(tokens.get(2))
            int height = Integer.parseInt(tokens.get(5))
            maxWidth = Math.max(maxWidth, width)
            maxHeight = Math.max(maxHeight, height)
        }
        properties << [ key : "cytomine.width", value : maxWidth ]
        properties << [ key : "cytomine.height", value : maxHeight ]
        properties << [ key : "cytomine.resolution", value : null ]
        properties << [ key : "cytomine.magnification", value : null ]
        return properties

    }

}
