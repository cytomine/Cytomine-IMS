package be.cytomine.formats.archive

import be.cytomine.exception.FormatException
import be.cytomine.formats.ArchiveFormat
import org.apache.commons.lang.RandomStringUtils
import utils.FilesUtils
import utils.ProcUtils

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
        mimeType = "application/zip"
    }

    boolean detect() {
        try{
            new ZipFile(absoluteFilePath)
        } catch(ZipException) {
            return false
        }
        return true
    }

    String[] extract(String destPath) {
                /*        long timestamp = new Date().getTime()
        String parentPath = new File(absoluteFilePath).getParent()
        String destPath = ["/tmp", timestamp].join(File.separator)*/

        /* Create and temporary directory which will contains the archive content */
        println "Create path=$destPath"
        ProcUtils.executeOnShell("mkdir -p " + destPath)
        println "Create right=$destPath"
        ProcUtils.executeOnShell("chmod -R 777 " + destPath)

        /* Get extension of filename in order to choose the uncompressor */
        String ext = FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase()
        /* Unzip */
        if (ext == 'zip') {
            /*def ant = new AntBuilder()
            ant.unzip(src : absoluteFilePath,
                    dest : destPath,
                    overwrite : false)*/
            def command = "unzip "+absoluteFilePath+" -d "+destPath
            println command
            def proc = command.execute()

            def sout = new StringBuilder(), serr = new StringBuilder()
            proc.consumeProcessOutput(sout, serr)
            proc.waitFor()
        } else{
            throw new FormatException("Zip has no zip extension")
        }

        def pathsAndExtensions = []
        new File(destPath).eachFileRecurse() { file ->
            if (!file.directory) {
                pathsAndExtensions << file.getAbsolutePath()
            }
        }

        return pathsAndExtensions
    }

    String[] convert() {

        println "in convert"
        println absoluteFilePath

        File current = new File(absoluteFilePath)
        String destPath = current.parent+"/" + current.name.substring(0,current.name.lastIndexOf("."))


        println current.parentFile.list()
        println destPath


        while(current.parentFile.list().contains(destPath)){
            println current.parentFile.list()
            println destPath

            destPath += "_converted"
        }

        /*        long timestamp = new Date().getTime()
String parentPath = new File(absoluteFilePath).getParent()
String destPath = ["/tmp", timestamp].join(File.separator)*/

        /* Create and temporary directory which will contains the archive content */
        println "Create path=$destPath"
        ProcUtils.executeOnShell("mkdir -p " + destPath)
        println "Create right=$destPath"
        ProcUtils.executeOnShell("chmod -R 777 " + destPath)

        /* Get extension of filename in order to choose the uncompressor */
        String ext = FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase()
        /* Unzip */
        if (ext == 'zip') {
            /*def ant = new AntBuilder()
            ant.unzip(src : absoluteFilePath,
                    dest : destPath,
                    overwrite : false)*/
            def command = "unzip "+absoluteFilePath+" -d "+destPath
            println command
            def proc = command.execute()

            def sout = new StringBuilder(), serr = new StringBuilder()
            proc.consumeProcessOutput(sout, serr)
            proc.waitFor()
        } else{
            throw new FormatException("Zip has no zip extension")
        }

        return [destPath]
    }
}
