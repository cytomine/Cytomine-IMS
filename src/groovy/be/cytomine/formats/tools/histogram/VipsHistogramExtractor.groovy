package be.cytomine.formats.tools.histogram

/*
 * Copyright (c) 2009-2020. Authors: see NOTICE file.
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

import be.cytomine.formats.tools.CytomineFile
import grails.util.Holders
import utils.ProcUtils

class VipsHistogramExtractor extends HistogramExtractor {

    private CytomineFile file

    VipsHistogramExtractor(def file) {
        this.file = file
    }

    @Override
    def histogram(int band) {
        def vipsExecutable = Holders.config.cytomine.ims.conversion.vips.executable
        def csvPath = this.file.absolutePath - ("."+this.file.extension()) + "-histogram-band-${band}.csv"
        def csvFile = new File(csvPath)

        def exec = ProcUtils.executeOnShell("$vipsExecutable hist_find ${file.absolutePath} ${csvFile.absolutePath} --band $band")

        if (exec.exit || !csvFile.exists())
            return []

        def lines = csvFile.readLines()
        def histogram = lines.first().split("\t").collect { Integer.parseInt(it) }


        csvFile.delete()


        return histogram
    }
}
