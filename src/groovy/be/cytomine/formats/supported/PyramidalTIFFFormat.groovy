package be.cytomine.formats.supported

import be.cytomine.formats.ITIFFFormat

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
import grails.util.Holders
import org.springframework.util.StringUtils
import utils.FilesUtils
import utils.ServerUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 28/04/14.
 */
class PyramidalTIFFFormat extends SupportedImageFormat implements ITIFFFormat {

    public PyramidalTIFFFormat () {
        extensions = ["tif", "tiff"]
        mimeType = "image/pyrtiff"
        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerBase)
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
        return this.detect(tiffinfo)
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
        properties << [ key : "cytomine.width", value : maxWidth ]
        properties << [ key : "cytomine.height", value : maxHeight ]
        properties << [ key : "cytomine.resolution", value : null/*unitConverter(resolution, unit)*/ ]
        properties << [ key : "cytomine.magnification", value : null ]
        return properties

    }

    public BufferedImage associated(String label) { //should be abstract
        if (label == "macro")  return null
        return thumb(256)
    }


    BufferedImage thumb(int maxSize) {
        String thumbURL = "${ServerUtils.getServer(iipURL)}?fif=$absoluteFilePath&SDS=0,90&CNT=1.0&HEI=$maxSize&WID=$maxSize&CVT=jpeg&QLT=99"
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

    boolean detect(String tiffinfo) {
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
}
