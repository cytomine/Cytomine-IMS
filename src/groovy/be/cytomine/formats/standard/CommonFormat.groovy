package be.cytomine.formats.standard

/*
 * Copyright (c) 2009-2015. Authors: see NOTICE file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import be.cytomine.formats.ImageFormat
import be.cytomine.formats.digitalpathology.VentanaTIFFFormat
import grails.util.Holders
import utils.FilesUtils
import utils.ProcUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 22/04/14.
 */
abstract class CommonFormat extends ImageFormat {

    public IMAGE_MAGICK_FORMAT_IDENTIFIER = null

    public boolean detect() {
        String extension = FilesUtils.getExtensionFromFilename(absoluteFilePath)

        if (new PyramidalTIFFFormat().extensions.contains(extension) || new VentanaTIFFFormat().extensions.contains(extension)) {
            return false //we do not run identify -verbose for TIFF files
        }

        def identifyExecutable = Holders.config.cytomine.identify
        String command = "$identifyExecutable -verbose $absoluteFilePath"
        def proc = command.execute()
        proc.waitFor()
        String stdout = proc.in.text
        return stdout.contains(IMAGE_MAGICK_FORMAT_IDENTIFIER)
    }

    public def convert(String workingPath) {
        String ext = FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase()
        String source = absoluteFilePath
        String target = [new File(absoluteFilePath).getParent(), UUID.randomUUID().toString() + ".tif"].join(File.separator)
        String intermediate = [new File(absoluteFilePath).getParent(), "_tmp.tif"].join(File.separator)

        println "ext : $ext"
        println "source : $source"
        println "target : $target"
        println "intermediate : $intermediate"

        //1. Look for vips executable
        def vipsExecutable = Holders.config.cytomine.vips

        //2. Pyramid command
        def pyramidCommand = """$vipsExecutable tiffsave "$source" "$target" --tile --pyramid --compression lzw --tile-width 256 --tile-height 256 --bigtiff"""

        boolean success = true

        success &= (ProcUtils.executeOnShell(pyramidCommand) == 0)

        if (success) {
            target
        }
    }

    public BufferedImage associated(String label) { //should be abstract
        if (label == "macro" || label == "preview") {
            thumb(256)
        } else if (label == "preview") {
            thumb(1024)
        }
    }

    public BufferedImage thumb(int maxSize) {
        def vipsThumbnailExecutable = Holders.config.cytomine.vipsthumbnail
        File thumbnailFile = File.createTempFile("thumbnail", ".jpg")
        def thumbnail_command = """$vipsThumbnailExecutable $absoluteFilePath --size $maxSize --interpolator bicubic --vips-concurrency=8 -o $thumbnailFile.absolutePath"""
        println thumbnail_command
        def proc = thumbnail_command.execute()
        proc.waitFor()
        return ImageIO.read(thumbnailFile)
    }
}
