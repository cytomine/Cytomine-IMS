package be.cytomine.formats.supported

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
import grails.util.Holders
import org.springframework.util.StringUtils
import utils.FilesUtils
import utils.ServerUtils
import utils.URLBuilder

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 28/04/14.
 */
class PyramidalTIFFFormat extends SupportedImageFormat {

    public PyramidalTIFFFormat () {
        extensions = ["tif", "tiff"]
        mimeType = "image/pyrtiff"
        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerCyto)
    }

    private excludeDescription = [
            "Not a TIFF",
            "<iScan",
            //"Hamamatsu",
            "Aperio",
            "Leica",
            "PHILIPS",
            "OME-XML",
            "Software: Adobe Photoshop"
    ]

    public boolean detect() {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        //we have a TIFF, but what kind ? flat, pyramid, multi-page, ventana ?

        boolean notTiff = false
        excludeDescription.each {
            notTiff |= tiffinfo.contains(it)
        }
        println "${tiffinfo.contains("Hamamatsu")}"
        println "${!FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase().equals("tif")}"
        println "notTiff=${notTiff}"
        if(tiffinfo.contains("Hamamatsu") && !FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase().equals("tif")) {
            return false //otherwise its a tiff file converted from ndpi
        }
        if (notTiff) return false

        int nbTiffDirectory = StringUtils.countOccurrencesOf(tiffinfo, "TIFF Directory")

        //pyramid or multi-page, sufficient ?
        if (nbTiffDirectory > 1) return true
        else if (nbTiffDirectory == 1) { //check if very small tiff
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

            return (maxWidth <= 256 && maxHeight <= 256)
        }


    }

    def properties() {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        def properties = [[key : "mimeType", value : mimeType]]
        int maxWidth = 0
        int maxHeight = 0
        def infos = tiffinfo.tokenize( '\n' );
        infos.findAll {
            it.contains 'Image Width:'
        }.each {
            def tokens = it.tokenize(" ")
            int width = Integer.parseInt(tokens.get(2))
            int height = Integer.parseInt(tokens.get(5))
            maxWidth = Math.max(maxWidth, width)
            maxHeight = Math.max(maxHeight, height)
        }

        Double resolution;
        String unit;
        def resolutions = infos.findAll {
            it.contains 'Resolution:'
        }.unique();
        if(resolutions.size() == 1){
            def tokens = resolutions[0].tokenize(" ,/")

            tokens.each {println it}

            resolution = Double.parseDouble(tokens.get(1))
            if(tokens.size() >= 5 && !tokens.get(3).contains("unitless")){
                unit = tokens.get(4)
            }
        }

        int maxDepth = 0
        infos.findAll {
            it.contains 'Bits/Sample:'
        }.each {
            def tokens = it.tokenize(" ")
            int depth = Integer.parseInt(tokens.get(1))
            maxDepth = Math.max(maxDepth, depth)
        }

        String colorspace
        def colorspaces = infos.findAll {
            it.contains 'Photometric Interpretation:'
        }.unique()
        if (colorspaces.size() == 1) {
            def tokens = colorspaces[0].tokenize(":")
            def value = tokens.get(1).trim().toLowerCase()
            if (value == "min-is-black" || value == "grayscale")
                colorspace = "grayscale"
            else if (value.contains("rgb"))
                colorspace = "rgb"
            else
                colorspace = value
        }

        properties << [ key : "cytomine.width", value : maxWidth ]
        properties << [ key : "cytomine.height", value : maxHeight ]
        properties << [ key : "cytomine.resolution", value : null/*unitConverter(resolution, unit)*/ ]
        properties << [ key : "cytomine.magnification", value : null ]
        properties << [ key : "cytomine.bitdepth", value : maxDepth]
        properties << [ key : "cytomine.colorspace", value : colorspace]
        return properties

    }

    public BufferedImage associated(String label) { //should be abstract
        if (label == "macro") {
            return thumb(256)
        }
    }


    BufferedImage thumb(int maxSize) {
        def iipRequest = new URLBuilder(ServerUtils.getServer(iipURL))
        iipRequest.addParameter("FIF", absoluteFilePath, true)
        iipRequest.addParameter("HEI", "$maxSize")
        iipRequest.addParameter("WID", "$maxSize")
        iipRequest.addParameter("QLT", "99")
        iipRequest.addParameter("CVT", "jpeg")
        String thumbURL = iipRequest.toString()
        println thumbURL
		return ImageIO.read(new URL(thumbURL))
    }

    //convert from pixel/unit to µm/pixel
    private Double unitConverter(Double res, String unit){
        if(res == null) return null;
        Double resOutput = res;
        if(unit == "inch"){
            resOutput /= 2.54
            unit = "cm"
        }
        if(unit == "cm"){
            resOutput = 1/resOutput
            resOutput*=10000
        } else{
            return null
        }
        return resOutput
    }


}
