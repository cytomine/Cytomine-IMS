package be.cytomine.formats.tools

/*
 * Copyright (c) 2009-2019. Authors: see NOTICE file.
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

import groovy.util.logging.Log4j
import utils.FilesUtils

@Log4j
class CytomineFile extends File {

    String tiffInfoOutput
    String imageMagickOutput
    String openSlideVendor
    String gdalInfoOutput

    def c
    def z
    def t

    CytomineFile(String pathname) {
        this(pathname, null, null, null)
    }

    CytomineFile(String pathname, def c, def z, def t) {
        super(pathname)
        setDimensions(c, z, t)
    }

    CytomineFile(String parent, String child) {
        this(parent, child, null, null, null)
    }

    CytomineFile(String parent, String child, def c, def z, def t) {
        super(parent, child)
        setDimensions(c, z, t)
    }

    CytomineFile(File parent, String child) {
        this(parent, child, null, null, null)
    }

    CytomineFile(File parent, String child, def c, def z, def t) {
        super(parent, child)
        setDimensions(c, z, t)
    }

    CytomineFile(CytomineFile file) {
        this(file.absolutePath, file.c, file.z, file.t)
    }

    def setDimensions(def c, def z, def t) {
        this.c = c
        this.z = z
        this.t = t
    }

    def getTiffInfoOutput() {
        if (!tiffInfoOutput)
            tiffInfoOutput = FormatUtils.getTiffInfo(this.absolutePath)
        return tiffInfoOutput
    }

    def getImageMagickOutput() {
        if (!imageMagickOutput)
            imageMagickOutput = FormatUtils.getImageMagick(this.absolutePath)
        return imageMagickOutput
    }

    def getOpenSlideVendor() {
        if (!openSlideVendor)
            openSlideVendor = FormatUtils.getOpenSlideVendor(this)
        return openSlideVendor
    }

    def getGdalInfoOutput() {
        if (!gdalInfoOutput)
            gdalInfoOutput = FormatUtils.getGdalInfo(this.absolutePath)
        return gdalInfoOutput
    }

    def extension() {
        return FilesUtils.getExtensionFromFilename(this.absolutePath).toLowerCase()
    }
}
