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

import be.cytomine.formats.tools.detectors.ImageMagickDetector
import com.pixelmed.dicom.AttributeList
import com.pixelmed.dicom.AttributeTag
import com.pixelmed.dicom.DicomDictionary
import com.vividsolutions.jts.geom.util.AffineTransformation
import com.vividsolutions.jts.io.WKTReader
import com.vividsolutions.jts.io.WKTWriter
import groovy.util.logging.Log4j
import utils.MimeTypeUtils
import utils.PropertyUtils

@Log4j
class DICOMFormat extends CommonFormat implements ImageMagickDetector {

    String IMAGE_MAGICK_FORMAT_IDENTIFIER = "DCM"

    DICOMFormat() {
        extensions = ["dcm", "dicom"]
        mimeType = MimeTypeUtils.MIMETYPE_DICOM

        // https://www.sno.phy.queensu.ca/~phil/exiftool/TagNames/DICOM.html
        cytominePropertyKeys[PropertyUtils.CYTO_WIDTH] = "DICOM.Columns"
        cytominePropertyKeys[PropertyUtils.CYTO_HEIGHT] = "DICOM.Rows"
        cytominePropertyKeys[PropertyUtils.CYTO_X_RES] = "DICOM.PixelSpacing" // first element
        cytominePropertyKeys[PropertyUtils.CYTO_Y_RES] = "DICOM.PixelSpacing" // second element
        cytominePropertyKeys[PropertyUtils.CYTO_BPS] = "DICOM.BitsAllocated"
        cytominePropertyKeys[PropertyUtils.CYTO_SPP] = "DICOM.SamplesPerPixel"
        cytominePropertyKeys[PropertyUtils.CYTO_COLORSPACE] = "DICOM.PhotometricInterpretation"

        cytominePropertyParsers[PropertyUtils.CYTO_X_RES] = { x ->
            def split = x.split("\\\\")
            if (split.size() > 0) return PropertyUtils.parseDouble(split[0])
            else null
        }

        cytominePropertyParsers[PropertyUtils.CYTO_Y_RES] = { x ->
            def split = x.split("\\\\")
            if (split.size() > 1) return PropertyUtils.parseDouble(split[1])
            else if (split.size() > 0) return PropertyUtils.parseDouble(split[0])
            else null
        }
    }

    def properties() {
        def properties = super.properties()
        def dictionary = new PropertyDictionary()
        def list = new AttributeList()
        list.read(this.file.absolutePath)
        (list.values() as ArrayList).each {
            def tag = dictionary.getNameFromTag(it.getTag())
            def key = "DICOM." + tag
            def value = it.getDelimitedStringValuesOrEmptyString()
            if (!tag?.isEmpty() && tag != "null" && tag && !value?.isEmpty())
                properties << [(key): value]
        }

        properties << [(PropertyUtils.CYTO_X_RES_UNIT): "mm"]
        properties << [(PropertyUtils.CYTO_Y_RES_UNIT): "mm"]

        return properties
    }

    def annotations() {
        def annotations = super.annotations()
        def dictionary = new AnnotationDictionary()
        AttributeList list = new AttributeList()
        list.read(this.file.absolutePath)
        def dicomAnnotations = list.get(dictionary.getTagFromName("Annotation.Definition"))
        if (dicomAnnotations) {
            def imageHeight = list.get(dictionary.getTagFromName("Rows")).getDelimitedStringValuesOrEmptyString()
            for (int i = 0; i < dicomAnnotations.getNumberOfItems(); i++) {
                AttributeList annotation = dicomAnnotations.getItem(i).getAttributeList()
                def wkt = annotation.get(dictionary.getTagFromName("Annotation.Polygon")).getDelimitedStringValuesOrEmptyString()
                def polygon = new WKTReader().read(wkt)
                def transformation = new AffineTransformation(0, 1, 0, -1, 0, Double.parseDouble(imageHeight.replaceAll(",", ".")))
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
//        super.createNameByTag();
        this.nameByTag = new HashMap(100)
        this.nameByTag.put(new AttributeTag(119, 16), "PrivateCreator[0]")
        this.nameByTag.put(new AttributeTag(119, 17), "PrivateCreator[1]")
        this.nameByTag.put(new AttributeTag(119, 6400), "Annotation.Number")
    }
}

class AnnotationDictionary extends DicomDictionary {
    @Override
    protected void createNameByTag() {
//        super.createNameByTag();
        this.nameByTag = new HashMap(100)
        this.nameByTag.put(new AttributeTag(119, 6400), "Annotation.Number")
        this.nameByTag.put(new AttributeTag(119, 6401), "Annotation.Definition")
        this.nameByTag.put(new AttributeTag(119, 6418), "Annotation.Row")
        this.nameByTag.put(new AttributeTag(119, 6419), "Annotation.Col")
        this.nameByTag.put(new AttributeTag(119, 6403), "Annotation.Indication")
        this.nameByTag.put(new AttributeTag(119, 6404), "Annotation.Severity")
        this.nameByTag.put(new AttributeTag(119, 6417), "Annotation.Polygon")
    }

    protected void createTagByName() {
//        super.createTagByName()
        this.tagByName = new HashMap(100)
        this.tagByName.put("Annotation.Number", new AttributeTag(119, 6400))
        this.tagByName.put("Annotation.Definition", new AttributeTag(119, 6401))
        this.tagByName.put("Annotation.Row", new AttributeTag(119, 6418))
        this.tagByName.put("Annotation.Col", new AttributeTag(119, 6419))
        this.tagByName.put("Annotation.Indication", new AttributeTag(119, 6403))
        this.tagByName.put("Annotation.Severity", new AttributeTag(119, 6404))
        this.tagByName.put("Annotation.Polygon", new AttributeTag(119, 6417))
    }
}