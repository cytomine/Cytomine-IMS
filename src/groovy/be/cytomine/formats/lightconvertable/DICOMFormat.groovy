package be.cytomine.formats.lightconvertable

import com.pixelmed.dicom.AttributeList
import com.pixelmed.dicom.AttributeTag
import com.pixelmed.dicom.DicomDictionary
import com.vividsolutions.jts.geom.util.AffineTransformation
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.io.WKTWriter

import be.cytomine.formats.ICommonFormat
import grails.util.Holders
import utils.ServerUtils

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

/**
 * Created by stevben on 22/04/14.
 */
class DICOMFormat extends CommonFormat implements ICommonFormat {

    public DICOMFormat() {
        extensions = ["dcm"]
        IMAGE_MAGICK_FORMAT_IDENTIFIER = "DCM"
        mimeType = "application/dicom"
        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerBase)
    }

    @Override
    def properties() {
        def properties = super.properties()
        def dictionary = new PropertyDictionary()
        def list = new AttributeList()
        list.read(absoluteFilePath)
        (list.values() as ArrayList).each {
            def tag = dictionary.getNameFromTag(it.getTag())
            def value = it.getDelimitedStringValuesOrEmptyString()
            if (!tag?.isEmpty() && tag != "null" && tag && !value?.isEmpty())
                properties << [key: "dicom.$tag", value: value]
        }

        return properties
    }

    def annotations() {
        def annotations = super.annotations()
        def dictionary = new AnnotationDictionary();
        AttributeList list = new AttributeList()
        list.read(absoluteFilePath)
        def dicomAnnotations = list.get(dictionary.getTagFromName("Annotation.Definition"))
        if (dicomAnnotations) {
            def imageHeight = list.get(dictionary.getTagFromName("Rows")).getDelimitedStringValuesOrEmptyString()
            for (int i=0; i < dicomAnnotations.getNumberOfItems(); i++) {
                AttributeList annotation = dicomAnnotations.getItem(i).getAttributeList()
                def wkt = annotation.get(dictionary.getTagFromName("Annotation.Polygon")).getDelimitedStringValuesOrEmptyString()
                def polygon = new WKTReader().read(wkt)
                def transformation = new AffineTransformation(0, 1, 0, -1, 0, Double.parseDouble(imageHeight))
                polygon.apply(transformation)
                def location = new WKTWriter().write(polygon)

                def term = annotation.get(dictionary.getTagFromName("Annotation.Indication")).getDelimitedStringValuesOrEmptyString()

                def properties = [:]
                properties << [severity: annotation.get(dictionary.getTagFromName("Annotation.Severity")).getDelimitedStringValuesOrEmptyString()]
                properties << [row: annotation.get(dictionary.getTagFromName("Annotation.Row")).getDelimitedStringValuesOrEmptyString()]
                properties << [col: annotation.get(dictionary.getTagFromName("Annotation.Col")).getDelimitedStringValuesOrEmptyString()]

                if (!location?.isEmpty())
                    annotations << [location: location, term: term, properties: properties]
            }
        }

        return annotations
    }
}

class PropertyDictionary extends DicomDictionary {
    @Override
    protected void createNameByTag() {
        super.createNameByTag();
        this.nameByTag.put(new AttributeTag(119, 16), "PrivateCreator[0]");
        this.nameByTag.put(new AttributeTag(119, 17), "PrivateCreator[1]");
        this.nameByTag.put(new AttributeTag(119, 6400), "Annotation.Number")
    }
}

class AnnotationDictionary extends DicomDictionary {
    @Override
    protected void createNameByTag() {
        super.createNameByTag();
        this.nameByTag.put(new AttributeTag(119, 6400), "Annotation.Number")
        this.nameByTag.put(new AttributeTag(119, 6401), "Annotation.Definition")
        this.nameByTag.put(new AttributeTag(119, 6418), "Annotation.Row")
        this.nameByTag.put(new AttributeTag(119, 6419), "Annotation.Col")
        this.nameByTag.put(new AttributeTag(119, 6403), "Annotation.Indication")
        this.nameByTag.put(new AttributeTag(119, 6404), "Annotation.Severity")
        this.nameByTag.put(new AttributeTag(119, 6417), "Annotation.Polygon")
    }

    protected void createTagByName() {
        super.createTagByName()
        this.tagByName.put("Annotation.Number", new AttributeTag(119, 6400))
        this.tagByName.put("Annotation.Definition", new AttributeTag(119, 6401))
        this.tagByName.put("Annotation.Row", new AttributeTag(119, 6418))
        this.tagByName.put("Annotation.Col", new AttributeTag(119, 6419))
        this.tagByName.put("Annotation.Indication", new AttributeTag(119, 6403))
        this.tagByName.put("Annotation.Severity", new AttributeTag(119, 6404))
        this.tagByName.put("Annotation.Polygon", new AttributeTag(119, 6417))
    }
}