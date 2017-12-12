package be.cytomine.formats.supported.digitalpathology

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

import java.awt.image.BufferedImage
import be.cytomine.formats.lightconvertable.ILightConvertableImageFormat
import grails.util.Holders
import utils.ServerUtils

/**
 * Created by stevben on 28/04/14.
 */
class VentanaTIFFFormat extends OpenSlideSingleFileFormat implements ILightConvertableImageFormat {

    public VentanaTIFFFormat() {
        extensions = ["tif", "vtif"]
        vendor = "ventana"
        mimeType = "openslide/ventana"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = "openslide.mpp-x"
        magnificiationProperty = "openslide.objective-power"
        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerCyto)
    }

    @Override
    def convert() {
        fakeExtension(absoluteFilePath)
        return absoluteFilePath
    }

    BufferedImage associated(String label) {
        BufferedImage bufferedImage = super.associated(label)
        if (label == "macro") {
            return rotate90ToRight(bufferedImage)
        } else {
            return bufferedImage
        }
    }

    String fakeExtension(def original) {
        def renamed = new File(original.take(original.lastIndexOf('.')) + ".vtif")
        if (!renamed.exists())
            "ln -s $original $renamed".execute()
        return renamed
    }

    @Override
    String tileURL(def fif, def params, def with_zoomify) {
        return super.tileURL(fakeExtension(fif), params, with_zoomify)
    }

    @Override
    String cropURL(def params, def charset) {
        params.fif = fakeExtension(params.fif)
        return super.cropURL(params, charset)
    }
}
