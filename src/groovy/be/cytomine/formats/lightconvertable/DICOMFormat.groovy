package be.cytomine.formats.lightconvertable

import com.pixelmed.dicom.AttributeList
import com.pixelmed.dicom.AttributeTag
import com.pixelmed.dicom.DicomDictionary

/*
 * Copyright (c) 2009-2017. Authors: see NOTICE file.
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

class DICOMFormat extends CommonFormat {

    public DICOMFormat() {
        extensions = ["dcm"]
        IMAGE_MAGICK_FORMAT_IDENTIFIER = "DCM"
//        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerBase)
    }

    @Override
    def properties() {
        def properties = super.properties()
        def dictionnary = new CustomDicomDictionary()
        def list = new AttributeList()
        list.read(absoluteFilePath)
        (list.values() as ArrayList).each {
            def tag = dictionnary.getNameFromTag(it.getTag())
            def value = it.getDelimitedStringValuesOrEmptyString()
            if (!tag?.isEmpty() && !value?.isEmpty())
                properties << [key: "dicom.$tag", value: value]
        }

        return properties
    }
}

class CustomDicomDictionary extends DicomDictionary {
    @Override
    protected void createNameByTag() {
        super.createNameByTag();
        this.nameByTag.put(new AttributeTag(119, 16), "PrivateCreator[0]");
        this.nameByTag.put(new AttributeTag(119, 17), "PrivateCreator[1]");
    }
}