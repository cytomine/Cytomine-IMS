package be.cytomine.formats.supported.digitalpathology

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

import be.cytomine.formats.supported.SupportedImageFormat
import org.openslide.AssociatedImage
import org.openslide.OpenSlide
import utils.ServerUtils
import utils.URLBuilder

import java.awt.image.BufferedImage

abstract class OpenSlideFormat extends SupportedImageFormat {

    protected String vendor = null

    String widthProperty = "openslide.level[0].width"
    String heightProperty = "openslide.level[0].height"

    boolean detect() {
        println "detect $absoluteFilePath"
        File slideFile = new File(absoluteFilePath)
        if (slideFile.canRead()) {
            try {
                println "can read $absoluteFilePath " +  OpenSlide.detectVendor(slideFile)
                return OpenSlide.detectVendor(slideFile) == vendor
            } catch (java.io.IOException e) {
                //Not a file that OpenSlide can recognize
                return false
            }
        } else {
            println "can not read $absoluteFilePath "
            return false
        }
    }

    public BufferedImage associated(String label) { //should be abstract
        File slideFile = new File(absoluteFilePath)
        BufferedImage associatedBufferedImage = null
        if (slideFile.canRead()) {
            OpenSlide openSlide = new OpenSlide(slideFile)
            openSlide.getAssociatedImages().each {
                if (it.key == label) {
                    AssociatedImage associatedImage = it.value
                    associatedBufferedImage = associatedImage.toBufferedImage()
                }
            }
            openSlide.close()
        }
        return associatedBufferedImage
    }

    public def properties() {
        File slideFile = new File(absoluteFilePath)
        def properties = [[key : "mimeType", value : mimeType]]
        try {
            if (slideFile.canRead()) {
                OpenSlide openSlide = new OpenSlide(slideFile)
                openSlide.getProperties().each {
                    properties << [key: it.key, value: it.value]
                }
                openSlide.close()
            } else {
                println "cannot read ${slideFile.absolutePath}"
            }
        }catch(Exception e) {
            println e
        }
        if (widthProperty && properties.find { it.key == widthProperty}?.value != null)
            properties << [ key : "cytomine.width", value : Integer.parseInt(properties.find { it.key == widthProperty}?.value) ]
        if (heightProperty && properties.find { it.key == heightProperty}?.value != null)
            properties << [ key : "cytomine.height", value : Integer.parseInt(properties.find { it.key == heightProperty}?.value) ]
        if (resolutionProperty && properties.find { it.key == resolutionProperty}?.value != null)
            properties << [ key : "cytomine.resolution", value : Double.parseDouble(properties.find { it.key == resolutionProperty}?.value) ]
        if (magnificiationProperty && properties.find { it.key == magnificiationProperty}?.value != null)
            properties << [ key : "cytomine.magnification", value : Double.parseDouble(properties.find { it.key == magnificiationProperty}?.value).intValue() ]

        def iipRequest = new URLBuilder(ServerUtils.getServer(iipURL))
        iipRequest.addParameter("FIF", absoluteFilePath, true)
        iipRequest.addParameter("obj", "IIP,1.0")
        iipRequest.addParameter("obj", "bits-per-channel")
        iipRequest.addParameter("obj", "colorspace")
        String propertiesURL = iipRequest.toString()
        String propertiesTextResponse = new URL(propertiesURL).text
        Integer depth = null
        String colorspace = null
        propertiesTextResponse.eachLine { line ->
            if (line.isEmpty()) return

            def args = line.split(":")
            if (args.length != 2) return

            if (args[0].equals('Bits-per-channel'))
                depth = Integer.parseInt(args[1])

            if (args[0].contains('Colorspace')) {
                def tokens = args[1].split(' ')
                if (tokens[2] == "1") {
                    colorspace = "grayscale"
                } else if (tokens[2] == "3") {
                    colorspace = "rgb"
                } else {
                    colorspace = "cielab"
                }
            }
        }
        properties << [ key : "cytomine.bitdepth", value : depth]
        properties << [ key : "cytomine.colorspace", value: colorspace]
        return properties
    }

    public BufferedImage thumb(int maxSize) {
        OpenSlide openSlide = new OpenSlide(new File(absoluteFilePath))
        BufferedImage thumbnail = openSlide.createThumbnailImage(0, 0, openSlide.getLevel0Width(), openSlide.getLevel0Height(), maxSize, BufferedImage.TYPE_INT_ARGB_PRE)
        openSlide.close()
        return thumbnail
    }
}
