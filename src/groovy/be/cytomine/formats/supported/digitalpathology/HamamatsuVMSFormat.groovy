package be.cytomine.formats.supported.digitalpathology


import be.cytomine.formats.CytomineFile
import be.cytomine.formats.MultipleFilesFormat
import be.cytomine.formats.detectors.OpenSlideDetector
import utils.MimeTypeUtils

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
/**
 * Created by stevben on 22/04/14.
 */
class HamamatsuVMSFormat extends OpenSlideFormat implements MultipleFilesFormat, OpenSlideDetector {

    String vendor = "hamamatsu"

    public HamamatsuVMSFormat() {
        extensions = ["vms"]
        mimeType = MimeTypeUtils.MIMETYPE_VMS

        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = null //to compute
        magnificationProperty = "hamamatsu.SourceLens"
    }

    @Override
    boolean detect() {
        File vms = getRootFile(this.file)
        log.info vms

        if(vms){
            this.file = new CytomineFile(vms.absolutePath)
            log.info this.file.openSlideVendor
            return OpenSlideDetector.super.detect()
        }
        return false
    }

    def properties() {
        def properties = super.properties()

        float physicalWidthProperty = Float.parseFloat(properties.find { it.key == "hamamatsu.PhysicalWidth"}.value.replaceAll(",","."))
        float widthProperty = Float.parseFloat(properties.find { it.key == "cytomine.width"}.value.replaceAll(",", "."))
        if (physicalWidthProperty && widthProperty) {
            def resolution = physicalWidthProperty / widthProperty / 1000
            properties << [ key : "cytomine.resolution", value : resolution]
        }
    }

    File getRootFile(File folder) {
        return folder.listFiles().find {file ->
            extensions.any {ext ->
                file.name.endsWith(".$ext")
            }
        }
    }
}
