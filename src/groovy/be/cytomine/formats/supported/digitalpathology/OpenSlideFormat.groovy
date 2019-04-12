package be.cytomine.formats.supported.digitalpathology

import be.cytomine.formats.detectors.OpenSlideDetector

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

import be.cytomine.formats.supported.NativeFormat
import org.openslide.AssociatedImage
import org.openslide.OpenSlide
import utils.ServerUtils
import utils.URLBuilder

import java.awt.image.BufferedImage

abstract class OpenSlideFormat extends NativeFormat /*implements OpenSlideDetector*/ {

    String vendor = null

    protected String widthProperty = "openslide.level[0].width"
    protected String heightProperty = "openslide.level[0].height"
    protected String resolutionProperty = null
    protected String magnificationProperty = null

    public BufferedImage associated(String label) { //should be abstract
        File slideFile = this.file
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

    @Override
    BufferedImage thumb(Object params) {
        return null
    }

    @Override
    BufferedImage associated(Object label) {
        return null
    }

    @Override
    String associated() {
        return null
    }

    public def properties() {
        File slideFile = this.file
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
            properties << [ key : "cytomine.resolution", value : Double.parseDouble(properties.find { it.key == resolutionProperty}?.value?.replaceAll(",",".")) ]
        if (magnificiationProperty && properties.find { it.key == magnificiationProperty}?.value != null)
            properties << [ key : "cytomine.magnification", value : Double.parseDouble(properties.find { it.key == magnificiationProperty}?.value?.replaceAll(",",".")).intValue() ]

        def iipRequest = new URLBuilder(ServerUtils.getServer(iipURL))
        iipRequest.addParameter("FIF", this.file.absolutePath, true)
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

    @Override
    String cropURL(Object params) {
        return null
    }

    @Override
    String tileURL(Object params) {
        return null
    }

    public BufferedImage thumb(int maxSize, def params=null) {
        OpenSlide openSlide = new OpenSlide(this.file)
        BufferedImage thumbnail = openSlide.createThumbnailImage(0, 0, openSlide.getLevel0Width(), openSlide.getLevel0Height(), maxSize, BufferedImage.TYPE_INT_ARGB_PRE)
        openSlide.close()
        return thumbnail
    }
}
