package be.cytomine.formats.lightconvertable

import be.cytomine.formats.IConvertableImageFormat
import be.cytomine.formats.supported.digitalpathology.OpenSlideSingleFileFormat
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
import java.awt.image.BufferedImage

/**
 * Created by stevben on 28/04/14.
 */
class VentanaTIFFFormat extends OpenSlideSingleFileFormat implements IConvertableImageFormat {

    public VentanaTIFFFormat() {
        extensions = ["tif", "vtif"]
        vendor = "ventana"
        mimeType = "openslide/ventana"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = "openslide.mpp-x"
        magnificiationProperty = "openslide.objective-power"
        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerVentana)
    }

    @Override
    def convert() {
        String source = absoluteFilePath
        String target = [new File(absoluteFilePath).getParent(), UUID.randomUUID().toString() + ".vtif"].join(File.separator)
        //make a symbolic link to the original file with a special extension 'vtif' in order to recognize the format within IIP.
        "ln -s $source $target".execute()
        return target
    }


    BufferedImage associated(String label) {
        BufferedImage bufferedImage = super.associated(label)
        if (label == "macro")
            return rotate90ToRight(bufferedImage)
        else
            return bufferedImage
    }
}
