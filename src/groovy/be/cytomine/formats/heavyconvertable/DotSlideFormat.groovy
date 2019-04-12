package be.cytomine.formats.heavyconvertable

import be.cytomine.exception.ConversionException
import be.cytomine.formats.CytomineFile
import be.cytomine.formats.Format
import be.cytomine.formats.MultipleFilesFormat
import be.cytomine.formats.NotNativeFormat
import utils.MimeTypeUtils
import utils.ProcUtils

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
 * Created by hoyoux on 28.04.15.
 */
class DotSlideFormat extends NotNativeFormat implements IHeavyConvertableImageFormat, MultipleFilesFormat {

    DotSlideFormat() {
        mimeType = MimeTypeUtils.MIMETYPE_DOTSLIDE
    }

    @Override
    boolean detect() {
        File target = getRootFile(file)
        if(!target) return false

        String command = "cat ${target.absolutePath}"
        return ProcUtils.executeOnShell(command).out.contains("dotSlide")
    }

    @Override
    def convert() {
        println "Conversion DotSlide : begin"
        String name = this.file.name

        // call the dotslide lib
        dotslide.Main.main("-fi", "${this.file.absolutePath}/fi",
                "-fp", "${this.file.absolutePath}/fp",
                "-p", "${this.file.absolutePath}/")

        dotslidebuild.Main.main("-f", "${this.file.absolutePath}/fp.txt",
                "-io", "${this.file.absolutePath}/$name")

        println "Conversion DotSlide : end"

        File target = new CytomineFile(this.file.parent, name + ".tif")
        if (!target)
            throw new ConversionException()

        return [target]
    }

    @Override
    File getRootFile(File folder) {
        return folder.listFiles().find {file ->
            file.isFile() && file.name == "ExtendedProps.xml"
        }
    }
}
