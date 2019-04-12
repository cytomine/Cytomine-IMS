package be.cytomine.formats.archive

import be.cytomine.exception.FormatException
import be.cytomine.formats.CytomineFile
import org.apache.commons.lang.RandomStringUtils
import utils.FilesUtils
import utils.ProcUtils

import java.util.zip.ZipException
import java.util.zip.ZipFile

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

class ZipFormat extends ArchiveFormat {

    ZipFormat() {
        extensions = ["zip"]
        mimeType = "application/zip"
    }

    boolean detect() {
        if (!this.extensions.any { it == this.file.extension()})
            return false
        
        try{
            new ZipFile(this.file)
        } 
        catch(ZipException ignored) {
            return false
        }
        
        return true
    }

    def convert() {
        CytomineFile target = new CytomineFile(this.file.parent, this.file.name - ".${this.file.extension()}")
        println target.path
        println this.file.parentFile.list()

        while (this.file.parentFile.list().contains(target.name)) {
            target = new CytomineFile(target.parent, "${target.name}_converted")
        }

        ProcUtils.executeOnShell("mkdir -p ${target.absolutePath}")
        ProcUtils.executeOnShell("chmod -R 777 ${target.absolutePath}")

        def command = """unzip ${this.file.absolutePath} -d ${target.absolutePath} """
        def proc = command.execute()
        def sout = new StringBuilder()
        def serr = new StringBuilder()
        proc.consumeProcessOutput(sout, serr)
        proc.waitFor()

        return [target]
    }
}
