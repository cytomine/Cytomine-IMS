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

import groovy.util.logging.Log4j
import utils.ProcUtils

@Log4j
trait CustomExtensionFormat {

    File rename() {
        String filename
        if (this.file.absolutePath.lastIndexOf('.') > -1)
            filename = this.file.absolutePath.substring(0, this.file.absolutePath.lastIndexOf('.')) + "." + this.customExtension
        else
            filename = this.file.absolutePath + "." + this.customExtension

        def renamed = new File(filename)
        if (!renamed.exists())
            ProcUtils.executeOnShell("ln -s ${this.file.absolutePath} ${renamed.absolutePath}")
        return renamed
    }
}