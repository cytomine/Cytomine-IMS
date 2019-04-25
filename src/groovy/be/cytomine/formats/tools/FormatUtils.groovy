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

import grails.util.Holders
import groovy.util.logging.Log4j
import org.openslide.OpenSlide
import utils.ProcUtils

@Log4j
class FormatUtils {

    static def getTiffInfo(def filePath) {
        def tiffinfoExecutable = Holders.config.cytomine.ims.detection.tiffinfo.executable
        def command = """$tiffinfoExecutable $filePath """
        return ProcUtils.executeOnShell(command).all
    }

    static def getImageMagick(def filePath) {
        def identifyExecutable = Holders.config.cytomine.ims.detection.identify.executable
        def command = """$identifyExecutable $filePath """
        return ProcUtils.executeOnShell(command).all
    }

    static def getGdalInfo(def filePath) {
        def executable = Holders.config.cytomine.ims.detection.gdal.executable
        def command = """$executable -json $filePath """
        return ProcUtils.executeOnShell(command).out
    }

    static def getOpenSlideVendor(def file) {
        if (!file.canRead()) {
            return false
        }

        try {
            return OpenSlide.detectVendor(file)
        } catch (IOException ignored) {
            //Not a file that OpenSlide can recognize
            return false
        }

    }
}
