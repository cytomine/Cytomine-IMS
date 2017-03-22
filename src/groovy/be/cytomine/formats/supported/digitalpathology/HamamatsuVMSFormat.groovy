package be.cytomine.formats.supported.digitalpathology

import org.openslide.OpenSlide

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
/**
 * Created by stevben on 22/04/14.
 */
class HamamatsuVMSFormat extends OpenSlideMultipleFileFormat {

    public HamamatsuVMSFormat() {
        extensions = ["vms"]
        vendor = "hamamatsu"
        mimeType = "openslide/vms"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = null //to compute
        magnificiationProperty = "hamamatsu.SourceLens"
    }

    @Override
    boolean detect() {
        File uploadedFile = new File(absoluteFilePath);
        File vms = uploadedFile.listFiles(). find { it.name.endsWith('.vms')}

        if(vms){
            absoluteFilePath = vms.absolutePath
            return super.detect()
        }
        return false
    }

    def properties() {
        def properties = super.properties()

        float physicalWidthProperty = Float.parseFloat(properties.find { it.key == "hamamatsu.PhysicalWidth"}.value)
        float widthProperty = (float) properties.find { it.key == "cytomine.width"}.value
        if (physicalWidthProperty && widthProperty) {
            def resolution = physicalWidthProperty / widthProperty / 1000
            properties << [ key : "cytomine.resolution", value : resolution]
        }
    }
}
