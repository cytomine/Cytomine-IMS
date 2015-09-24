package be.cytomine.formats.specialtiff

import grails.util.Holders

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


/**
 * Created by hoyoux on 16.02.15.
 */
class BrokenTIFFFormat extends TIFFToConvert {

    public BrokenTIFFFormat () {
        extensions = ["tif", "tiff"]
    }

    public boolean detect() {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        def process = "$tiffinfoExecutable $absoluteFilePath".execute()

        BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErr()));

        String err;
        while((err = errReader.readLine()) != null) {
            println "tiffinfo error :"
            println err
            if(err.contains("not a valid IFD offset.")) return true;
        }
        return false
    }
}
