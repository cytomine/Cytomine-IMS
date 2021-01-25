package be.cytomine.formats.archive

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

import be.cytomine.exception.ConversionException
import be.cytomine.formats.tools.CytomineFile
import grails.util.Holders
import groovy.util.logging.Log4j
import utils.MimeTypeUtils
import utils.ProcUtils

@Log4j
class ZipFormat extends ArchiveFormat {

    ZipFormat() {
        extensions = ["zip"]
        mimeType = MimeTypeUtils.MIMETYPE_ZIP
    }

    boolean detect() {
        if (!this.extensions.any { it == this.file.extension() })
            return false

        return ProcUtils.executeOnShell("unzip -t " + this.file.absolutePath).exit == 0
    }

    def convert() {
        CytomineFile target = new CytomineFile(this.file.parent, this.file.name - ".${this.file.extension()}")

        while (this.file.parentFile.list().contains(target.name)) {
            target = new CytomineFile(target.parent, "${target.name}_converted")
        }

        ProcUtils.executeOnShell("mkdir -p ${target.absolutePath}")
        ProcUtils.executeOnShell("chmod -R 777 ${target.absolutePath}")

        def executable = Holders.config.cytomine.ims.conversion.unzip.executable
        def command = """$executable ${this.file.absolutePath} -d ${target.absolutePath} """
        if (ProcUtils.executeOnShell(command).exit != 0 || !target.exists())
            throw new ConversionException("${file.absolutePath} hasn't been converted to ${target.absolutePath}")

        return [target]
    }
}
