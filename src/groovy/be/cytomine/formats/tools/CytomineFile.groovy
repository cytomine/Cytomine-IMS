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
    String ffProbeOutput

    def c
    def z
    def t
    def channelName

    CytomineFile(String pathname) {
        this(pathname, null, null, null)
    }

    CytomineFile(String pathname, def c, def z, def t) {
        this(pathname,c, z, t, null)
    }

    CytomineFile(String pathname, def c, def z, def t, def channelName) {
        super(pathname)
        setDimensions(c, z, t)
        this.channelName = channelName
    }

    CytomineFile(String parent, String child) {
        this(parent, child, null, null, null)
    }

    CytomineFile(String parent, String child, def c, def z, def t) {
        this(parent, child, c, z, t, null)
    }

    CytomineFile(String parent, String child, def c, def z, def t, def channelName) {
        super(parent, child)
        setDimensions(c, z, t)
        this.channelName = channelName
    }

    CytomineFile(File parent, String child) {
        this(parent, child, null, null, null)
    }

    CytomineFile(File parent, String child, def c, def z, def t) {
        this(parent, child, c, z, t, null)
    }

    CytomineFile(File parent, String child, def c, def z, def t, def channelName) {
        super(parent, child)
        setDimensions(c, z, t)
        this.channelName = channelName
    }

    CytomineFile(CytomineFile file) {
        this(file.absolutePath, file.c, file.z, file.t, file.channelName)
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
        // Hack to avoid video in image magick which crashes with ffmpeg (to investigate)
        def videoExtensions = ["mp4", "mov", "avi", "flv", "m4a", "3gp", "3g2", "mj2"]
        if (!imageMagickOutput && !videoExtensions.contains(extension()))
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

    def getFfProbeOutput() {
        if (!ffProbeOutput)
            ffProbeOutput = FormatUtils.getFfProbe(this.absolutePath)
        return ffProbeOutput
    }

    def extension() {
        return FilesUtils.getExtensionFromFilename(this.absolutePath).toLowerCase()
    }
}
