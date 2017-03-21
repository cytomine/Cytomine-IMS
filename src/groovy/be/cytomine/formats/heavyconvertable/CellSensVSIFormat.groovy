package be.cytomine.formats.heavyconvertable
import be.cytomine.formats.Format

/*
 * Copyright (c) 2009-2016. Authors: see NOTICE file.
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
class CellSensVSIFormat extends BioFormatConvertable {

    @Override
    boolean detect() {
        File folder = new File(absoluteFilePath)

        File target = folder.listFiles().find {it.isFile() && it.absolutePath.endsWith(".vsi")}
        if(target) absoluteFilePath = target.absolutePath
        return target != null;
    }

    @Override
    boolean getGroup() {
        return false
    }
}
