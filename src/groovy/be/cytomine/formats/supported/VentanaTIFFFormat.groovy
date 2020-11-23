package be.cytomine.formats.supported

import be.cytomine.formats.supported.digitalpathology.OpenSlideSingleFileFormat
import grails.util.Holders
import utils.ProcUtils
import utils.ServerUtils

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
 * Created by stevben on 28/04/14.
 */
class VentanaTIFFFormat extends OpenSlideSingleFileFormat {

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

    BufferedImage associated(String label) {
        BufferedImage bufferedImage = super.associated(label)
        if (label == "macro") {
            return rotate90ToRight(bufferedImage)
        } else {
            return bufferedImage
        }
    }

    @Override
    boolean detect() {
        if(super.detect()) {
            def filename
            if (absoluteFilePath.lastIndexOf('.') > -1)
                filename = absoluteFilePath.substring(0, absoluteFilePath.lastIndexOf('.')) + ".vtif"
            else
                filename = absoluteFilePath + ".vtif"

            filename = filename.replace(";", "\\;")
            filename = filename.replace("&", "\\&")
            String originalFilePath = absoluteFilePath.replace(";", "\\;").replace("&", "\\&")
            def renamed = new File(filename)
            if (!renamed.exists())
                ProcUtils.executeOnShell("ln -s ${originalFilePath} ${renamed.absolutePath}")
            return renamed

        }
    }

    @Override
    String cropURL(def params, def charset = "UTF-8") {
        String fif = params.fif
        if (fif.lastIndexOf('.') > -1)
            fif = fif.substring(0, fif.lastIndexOf('.')) + ".vtif"
        else
            fif = fif + ".vtif"

        params.fif = fif
        return super.cropURL(params, charset)
    }

    @Override public String tileURL(fif, params) {

        if (fif.lastIndexOf('.') > -1)
            fif = fif.substring(0, fif.lastIndexOf('.')) + ".vtif"
        else
            fif = fif + ".vtif"

        return super.tileURL(fif, params)
    }
}
