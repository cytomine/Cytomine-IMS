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

import groovy.util.logging.Log4j
import utils.MimeTypeUtils

@Log4j
class ZeissCZIFormat extends BioFormatConvertable {
    final String CZI_MAGIC_STRING = "ZISRAWFILE"

    ZeissCZIFormat() {
        super()
        extensions = ["czi"]
        mimeType = MimeTypeUtils.MIMETYPE_ZEISSCZI
    }

    @Override
    boolean detect() {
        int blockLen = 10
        String magic = null
        FileInputStream fin = null
        try {
            fin = new FileInputStream(file)
            byte[] fileContent = new byte[blockLen]
            fin.read(fileContent)
            magic = new String(fileContent)
        } finally {
            if (fin != null) fin.close()
        }

        return magic == CZI_MAGIC_STRING
    }

    @Override
    boolean getGroup() {
        return true
    }

    @Override
    boolean getOnlyBiggestSerie() {
        return true
    }

    @Override
    boolean includeRawProperties() {
        return true
    }
}

