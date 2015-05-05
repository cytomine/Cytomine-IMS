package be.cytomine.formats.digitalpathology

import be.cytomine.formats.ImageFormat
import org.openslide.AssociatedImage

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
import org.openslide.OpenSlide

import java.awt.image.BufferedImage

/**
 * Created by stevben on 22/04/14.
 */
abstract class OpenSlideFormat extends ImageFormat {

    protected String vendor = null

    String widthProperty = "openslide.level[0].width"
    String heightProperty = "openslide.level[0].height"

    boolean detect() {
        println "detect $absoluteFilePath"
        File slideFile = new File(absoluteFilePath)
        if (slideFile.canRead()) {
            println "can read $absoluteFilePath " +  OpenSlide.detectVendor(slideFile)
            try {
                return OpenSlide.detectVendor(slideFile) == vendor
            } catch (java.io.IOException e) {
                //Not a file that OpenSlide can recognize
                return false
            }
        } else {
            //throw ERROR reading file
        }

    }

    public def convert(String workingPath) {
        return null //nothing to do, the format is understood by IIP+OpenSlide natively
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
        if (associatedBufferedImage) return associatedBufferedImage
        else return null
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
        println properties
        if (widthProperty && properties.find { it.key == widthProperty}?.value != null)
            properties << [ key : "cytomine.width", value : Integer.parseInt(properties.find { it.key == widthProperty}?.value) ]
        if (heightProperty && properties.find { it.key == heightProperty}?.value != null)
            properties << [ key : "cytomine.height", value : Integer.parseInt(properties.find { it.key == heightProperty}?.value) ]
        if (resolutionProperty && properties.find { it.key == resolutionProperty}?.value != null)
            properties << [ key : "cytomine.resolution", value : Double.parseDouble(properties.find { it.key == resolutionProperty}?.value) ]
        if (magnificiationProperty && properties.find { it.key == magnificiationProperty}?.value != null)
            properties << [ key : "cytomine.magnification", value : Double.parseDouble(properties.find { it.key == magnificiationProperty}?.value).intValue() ]

        return properties
    }

    public BufferedImage thumb(int maxSize) {
        OpenSlide openSlide = new OpenSlide(new File(absoluteFilePath))
        BufferedImage thumbnail = openSlide.createThumbnailImage(maxSize)
        openSlide.close()
        return thumbnail

    }


}
