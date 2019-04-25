package be.cytomine.formats.tools.metadata

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

import be.cytomine.formats.tools.CytomineFile
import groovy.util.logging.Log4j

@Log4j
class TiffInfoMetadataExtractor extends MetadataExtractor {

    private CytomineFile file

    TiffInfoMetadataExtractor(def file) {
        this.file = file
    }

    // https://www.awaresystems.be/imaging/tiff/tifftags/baseline.html
    def properties() {
        def infos = this.file.getTiffInfoOutput().tokenize('\n')
        def properties = [:]

        // Width and height
        int maxWidth = 0
        int maxHeight = 0
        infos.findAll { it.contains('Image Width:') }.each {
            def tokens = it.tokenize(" ")
            int width = Integer.parseInt(tokens.get(2))
            int height = Integer.parseInt(tokens.get(5))
            maxWidth = Math.max(maxWidth, width)
            maxHeight = Math.max(maxHeight, height)
        }
        properties << ["cytomine.width": maxWidth]
        properties << ["cytomine.height": maxHeight]

        // Resolution
        def resolutions = infos.findAll { it.contains('Resolution:') }.unique()
        if (resolutions.size() == 1) {
            def tokens = resolutions[0].tokenize(" ,/")
            properties << ["cytomine.physicalSizeX": Double.parseDouble(tokens.get(1).replaceAll(",", "."))]
            properties << ["cytomine.physicalSizeY": Double.parseDouble(tokens.get(3).replaceAll(",", "."))]
            if (tokens.size() >= 5 && !tokens.get(3).contains("unitless")) {
                def unit = tokens.get(4)
                properties << ["cytomine.physicalSizeXUnit": unit]
                properties << ["cytomine.physicalSizeYUnit": unit]
            }
        }

        // Bit/Sample
        def bps = infos.findAll { it.contains('Bit/Sample:') }.unique()
        if (bps.size() == 1) {
            def tokens = bps[0].tokenize(" ")
            properties << ["cytomine.bitPerSample": Integer.parseInt(tokens.get(1))]
        }

        // Sample/pixel
        def spp = infos.findAll { it.contains('Sample/Pixel:') }.unique()
        if (spp.size() == 1) {
            def tokens = spp[0].tokenize(" ")
            properties << ["cytomine.samplePerPixel": Integer.parseInt(tokens.get(1))]
        }

        // Colorspace
        def colorspaces = infos.findAll { it.contains('Photometric Interpretation:') }.unique()
        if (colorspaces.size() == 1) {
            def tokens = colorspaces[0].tokenize(":")
            def value = tokens.get(1).trim().toLowerCase()
            String colorspace
            if (value == "min-is-black" || value == "grayscale")
                colorspace = "grayscale"
            else if (value.contains("rgb"))
                colorspace = "rgb"
            else
                colorspace = value
            properties << ["cytomine.colorspace": colorspace]
        }

        return properties
    }
}
