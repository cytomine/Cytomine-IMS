package be.cytomine.formats.heavyconvertable

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

import be.cytomine.formats.tools.CytomineFile
import be.cytomine.formats.tools.MultipleFilesFormat
import groovy.util.logging.Log4j
import utils.MimeTypeUtils

@Log4j
class CellSensVSIFormat extends BioFormatConvertable implements MultipleFilesFormat {

    CellSensVSIFormat() {
        super()
        extensions = ["vsi"]
        mimeType = MimeTypeUtils.MIMETYPE_VSI
    }

    @Override
    boolean detect() {
        File vsi = getRootFile(this.file)

        if (vsi) {
            this.file = new CytomineFile(vsi.absolutePath)
        }

        return extensions.any { ext ->
            this.file.name.endsWith(".$ext")
        }
    }

    @Override
    boolean getGroup() {
        return false
    }

    @Override
    boolean getOnlyBiggestSerie() {
        return true
    }

    @Override
    File getRootFile(File folder) {
        return folder.listFiles().find { file ->
            file.isFile() && extensions.any { ext ->
                file.name.endsWith(".$ext")
            }
        }
    }
}
