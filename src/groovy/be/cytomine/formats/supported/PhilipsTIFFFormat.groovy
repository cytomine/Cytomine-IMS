package be.cytomine.formats.supported

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
import be.cytomine.formats.supported.digitalpathology.OpenSlideSingleFileFormat
import grails.util.Holders
import utils.ProcUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 12/07/14.
 */
class PhilipsTIFFFormat extends OpenSlideSingleFileFormat{

    public PhilipsTIFFFormat() {
        extensions = ["tiff"]
        vendor = "philips"
        mimeType = "philips/tif"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = "openslide.mpp-x"
        magnificiationProperty = null
    }

    BufferedImage associated(String label) {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        int numberOfTIFFDirectories = tiffinfo.count("TIFF Directory")
        if (label == "label") {
            //last directory
            getTIFFSubImage(numberOfTIFFDirectories - 1)
        } else if (label == "macro") {
            //next to last directory
            getTIFFSubImage(numberOfTIFFDirectories - 2)
        } else {
            thumb(512)
        }
    }
    private BufferedImage getTIFFSubImage(int index) {
        boolean convertSuccessfull = true

        println ImageIO.getReaderFormatNames()
        String source = absoluteFilePath
        File target = File.createTempFile("label", ".jpg")
        String targetPath = target.absolutePath

        println "target=" + target.getPath()
        def vipsExecutable = Holders.config.cytomine.vips
        def command = """$vipsExecutable im_copy $source:$index $targetPath"""
        println command
        convertSuccessfull &= ProcUtils.executeOnShell(command) == 0

        BufferedImage labelImage = null
        if (convertSuccessfull) {
            println targetPath
            println new File(targetPath).exists()
            labelImage = ImageIO.read(target)
            //labelImage = rotate90ToRight(labelImage)
            assert(labelImage)
        }
        target.delete()
        return labelImage
    }

    @Override
    boolean detect() {
        if(super.detect()) {
            def filename
            if (absoluteFilePath.lastIndexOf('.') > -1)
                filename = absoluteFilePath.substring(0, absoluteFilePath.lastIndexOf('.')) + ".ptiff"
            else
                filename = absoluteFilePath + ".ptiff"

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
            fif = fif.substring(0, fif.lastIndexOf('.')) + ".ptiff"
        else
            fif = fif + ".ptiff"

        params.fif = fif
        return super.cropURL(params, charset)
    }

    @Override public String tileURL(fif, params) {

        if (fif.lastIndexOf('.') > -1)
            fif = fif.substring(0, fif.lastIndexOf('.')) + ".ptiff"
        else
            fif = fif + ".ptiff"

        return super.tileURL(fif, params)
    }
}
