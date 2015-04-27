package be.cytomine.formats.digitalpathology

/*
 * Copyright (c) 2009-2015. Authors: see NOTICE file.
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

import be.cytomine.formats.standard.TIFFFormat
import grails.util.Holders
import utils.ProcUtils

import java.awt.image.BufferedImage

/**
 * Created by stevben on 12/07/14.
 */
class PhilipsTIFFFormat extends TIFFFormat{

    public PhilipsTIFFFormat() {
        extensions = ["tiff"]
        mimeType = "philips/tif"
        widthProperty = null //to compute
        heightProperty = null //to compute
        resolutionProperty = null
        magnificiationProperty = null
    }

    private excludeDescription = [
            "Not a TIFF",
            "Make: Hamamatsu",
            "Leica",
            "ImageDescription: Aperio Image Library"
    ]

    public boolean detect() {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text

        boolean notTiff = false
        excludeDescription.each {
            notTiff |= tiffinfo.contains(it)
        }
        if (notTiff) return false

        return (tiffinfo.contains("PHILIPS")) //DICOM_MANUFACTURER xml field
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

    BufferedImage thumb(int maxSize) {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        int numberOfTIFFDirectories = tiffinfo.count("TIFF Directory")
        getTIFFSubImage(numberOfTIFFDirectories - 4)

        //:to do - scale the image to maxSize
    }

    public def convert(String workingPath) {
        boolean convertSuccessfull = true

        String source = absoluteFilePath
        String target = [new File(absoluteFilePath).getParent(), UUID.randomUUID().toString() + ".tif"].join(File.separator)

        def vipsExecutable = Holders.config.cytomine.vips
        def command = """$vipsExecutable im_vips2tiff $source:0 $target:jpeg:95,tile:256x256,pyramid,,,,8"""
        convertSuccessfull &= ProcUtils.executeOnShell(command) == 0

        if (convertSuccessfull) {
            return target
        }

    }

    public def properties() {
        File slideFile = new File(absoluteFilePath)
        def properties = [[key : "mimeType", value : mimeType]]
        if (slideFile.canRead()) {
            def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
            String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
            //get width & height from tiffinfo...
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
        }


    }
}
