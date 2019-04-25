package utils

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

class PropertyUtils {

    public static final String CYTO_WIDTH = "cytomine.width"
    public static final String CYTO_HEIGHT = "cytomine.height"
    public static final String CYTO_DEPTH = "cytomine.depth"
    public static final String CYTO_DURATION = "cytomine.duration"
    public static final String CYTO_CHANNELS = "cytomine.channels"
    public static final String CYTO_X_RES = "cytomine.physicalSizeX"
    public static final String CYTO_Y_RES = "cytomine.physicalSizeY"
    public static final String CYTO_Z_RES = "cytomine.physicalSizeZ"
    public static final String CYTO_FPS = "cytomine.fps"
    public static final String CYTO_MAGNIFICATION = "cytomine.magnification"
    public static final String CYTO_BPS = "cytomine.bitPerSample"
    public static final String CYTO_SPP = "cytomine.samplePerPixel"
    public static final String CYTO_COLORSPACE = "cytomine.colorspace"
    public static final String CYTO_X_RES_UNIT = "cytomine.physicalSizeXUnit"
    public static final String CYTO_Y_RES_UNIT = "cytomine.physicalSizeYUnit"
    public static final String CYTO_Z_RES_UNIT = "cytomine.physicalSizeZUnit"
    public static final String CYTO_MIMETYPE = "cytomine.mimeType"
    public static final String CYTO_EXT = "cytomine.extension"
    public static final String CYTO_FORMAT = "cytomine.format"

    public static final def parseString = {
        x -> x?.toString()
    }

    public static final def parseInt = { x ->
        if (x instanceof Integer)
            return x

        try {
            return Integer.parseInt(x?.toString())
        }
        catch (NumberFormatException ignored) {
            return null
        }
    }

    public static final def parseDouble = { x ->
        if (x instanceof Double)
            return x

        try {
            return Double.parseDouble(x?.toString()?.replaceAll(",", "."))
        }
        catch (NumberFormatException ignored) {
            return null
        }
    }

    public static final def parseIntFirstWord = { x ->
        def split = (x as String).split(" ")
        if (split.size() == 0)
            return null

        return PropertyUtils.parseInt(split[0])
    }
}
