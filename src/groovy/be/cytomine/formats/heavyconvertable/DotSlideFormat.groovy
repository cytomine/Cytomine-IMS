package be.cytomine.formats.heavyconvertable

import be.cytomine.formats.Format

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
 * Created by hoyoux on 28.04.15.
 */
class DotSlideFormat extends Format implements IHeavyConvertableImageFormat {

    DotSlideFormat() {
        mimeType = "olympus/.slide"
    }

    @Override
    boolean detect() {
        String mainFile = "ExtendedProps.xml"
        File folder = new File(absoluteFilePath)

        File target = folder.listFiles().find {it.name.equals(mainFile)}
        if(!target) return false

        String command = "cat  "+target.absolutePath
        def proc = command.execute()
        proc.waitFor()
        String stdout = proc.in.text
        return stdout.contains("dotSlide")
    }

    @Override
    String[] convert() {
        println "Conversion DotSlide : begin"
        String name = new File(absoluteFilePath).name

        // call the dotslide lib
        dotslide.Main.main("-fi", "$absoluteFilePath/fi", "-fp", "$absoluteFilePath/fp" , "-p", "$absoluteFilePath/");
        dotslidebuild.Main.main("-f", "$absoluteFilePath/fp.txt", "-io", "$absoluteFilePath/$name")

        println "Conversion DotSlide : end"
        return [absoluteFilePath+"/"+name+".tif"]
    }
}
