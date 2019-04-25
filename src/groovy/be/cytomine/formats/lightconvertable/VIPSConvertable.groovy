package be.cytomine.formats.lightconvertable

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

import be.cytomine.exception.ConversionException
import be.cytomine.formats.tools.CytomineFile
import be.cytomine.formats.NotNativeFormat
import grails.util.Holders
import groovy.util.logging.Log4j
import utils.FilesUtils
import utils.ProcUtils

@Log4j
abstract class VIPSConvertable extends NotNativeFormat {

    @Override
    def convert() {
        String targetName = (this.file.name - ".${this.file.extension()}") + "_pyr.tif"
        CytomineFile target = new CytomineFile(this.file.parent, FilesUtils.correctFilename(targetName), this.file.c, this.file.z, this.file.t)

        return [convertToPyramidalTIFF(file, target)]
    }

    static def convertToPyramidalTIFF(CytomineFile source, CytomineFile target) {

        def vipsExecutable = Holders.config.cytomine.ims.conversion.vips.executable
        def compression = Holders.config.cytomine.ims.conversion.vips.compression ?: "jpeg -Q 95"
        def tileSize = 256

        def command = """$vipsExecutable tiffsave $source.absolutePath $target.absolutePath --bigtiff --tile --tile-width $tileSize --tile-height $tileSize --pyramid --compression $compression"""

        if (ProcUtils.executeOnShell(command).exit != 0 || !target.exists())
            throw new ConversionException("${source.absolutePath} hasn't been converted to ${target.absolutePath}")

        return target
    }
}
