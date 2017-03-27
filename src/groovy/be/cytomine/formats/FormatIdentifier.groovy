package be.cytomine.formats

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

import be.cytomine.exception.FormatException
import be.cytomine.formats.archive.ZipFormat
import be.cytomine.formats.heavyconvertable.OMETIFFFormat
import be.cytomine.formats.heavyconvertable.CellSensVSIFormat
import be.cytomine.formats.heavyconvertable.DotSlideFormat
import be.cytomine.formats.heavyconvertable.ZeissCZIFormat
import be.cytomine.formats.lightconvertable.BMPFormat
import be.cytomine.formats.lightconvertable.DICOMFormat
import be.cytomine.formats.lightconvertable.JPEGFormat
import be.cytomine.formats.lightconvertable.PGMFormat
import be.cytomine.formats.lightconvertable.PNGFormat
import be.cytomine.formats.lightconvertable.specialtiff.BrokenTIFFFormat
import be.cytomine.formats.lightconvertable.specialtiff.CZITIFFFormat
import be.cytomine.formats.lightconvertable.specialtiff.HuronTIFFFormat
import be.cytomine.formats.lightconvertable.specialtiff.PlanarTIFFFormat
import be.cytomine.formats.lightconvertable.specialtiff.PhotoshopTIFFFormat
import be.cytomine.formats.supported.VentanaTIFFFormat
import be.cytomine.formats.supported.JPEG2000Format
import be.cytomine.formats.supported.PhilipsTIFFFormat
import be.cytomine.formats.supported.PyramidalTIFFFormat
import be.cytomine.formats.supported.VentanaBIFFormat
import be.cytomine.formats.supported.digitalpathology.*
import be.cytomine.formats.supported.SupportedImageFormat
import org.apache.commons.lang.RandomStringUtils

/**
 * Created by stevben on 22/04/14.
 */
public class FormatIdentifier {

    static public getAvailableArchiveFormats() {
        return [
                new ZipFormat()
        ]
    }

    static public getAvailableMultipleImageFormats() {
        return [
                //openslide compatibles formats
                new HamamatsuVMSFormat(),
                new MiraxMRXSFormat(),
                new SakuraSVSlideFormat()
        ]
    }

    static public getAvailableHierarchicalMultipleImageFormats() {
        return [
                new DotSlideFormat(),
                new CellSensVSIFormat()
        ]
    }

    static public getAvailableSingleFileImageFormats() {
        //check the extension and or content in order to identify the right Format
        return [
                new ZeissCZIFormat(),
                //openslide compatibles formats
                new AperioSVSFormat(),
                new HamamatsuNDPIFormat(),
                new LeicaSCNFormat(),
                //new SakuraSVSlideFormat(),
                new PhilipsTIFFFormat(),
                new CZITIFFFormat(),
                new OMETIFFFormat(),
                //common formats
                new OMETIFFFormat(),
                new PhotoshopTIFFFormat(),
                new HuronTIFFFormat(),
                new PlanarTIFFFormat(),
                new BrokenTIFFFormat(),
                new PyramidalTIFFFormat(),
                new VentanaBIFFormat(),
                new VentanaTIFFFormat(),
                new JPEG2000Format(),
                new DICOMFormat(),
                new JPEGFormat(),
                new PGMFormat(),
                new PNGFormat(),
                new BMPFormat()
        ]
    }

    static public def getImageFormats(String uploadedFilePath, def imageFormats = [], def parent = null) {

        File uploadedFile = new File(uploadedFilePath);

        if(uploadedFile.isDirectory()){
            println "$uploadedFilePath is a directory"

            if(uploadedFile.name == "__MACOSX") return;
            // check if it is a folder containing one multipleFileImage
            def multipleFileImageFormats = getAvailableHierarchicalMultipleImageFormats() + getAvailableMultipleImageFormats()

            def format = multipleFileImageFormats.find { imageFormat ->
                imageFormat.absoluteFilePath = uploadedFilePath
                return imageFormat.detect()
            }

            if(format){
                imageFormats << [
                        absoluteFilePath : format.absoluteFilePath,
                        imageFormat : format,
                        parent : parent
                ]
            } else {
                for(File child : uploadedFile.listFiles()) getImageFormats(child.absolutePath, imageFormats, parent);
            }
            return imageFormats
        }

        def archiveFormats = getAvailableArchiveFormats()

        archiveFormats.each {
            it.absoluteFilePath = uploadedFilePath
        }

        ArchiveFormat detectedArchiveFormat = archiveFormats.find {
            it.detect()
        }

        if (detectedArchiveFormat) { //archive, we need to extract and analyze the content

            String dest = uploadedFile.getParent()+ "/" + RandomStringUtils.random(13,  (('A'..'Z') + ('0'..'0')).join().toCharArray())
            detectedArchiveFormat.extract(dest)

            getImageFormats(dest,imageFormats, [absoluteFilePath : uploadedFilePath, imageFormat : detectedArchiveFormat])

        } else {
            imageFormats << [
                    uploadedFilePath : uploadedFilePath,
                    imageFormat : getImageFormat(uploadedFilePath),
                    parent : parent
            ]
        }
        return imageFormats
    }

    static public SupportedImageFormat getImageFormatByMimeType(String fif, String mimeType) {
        def imageFormats = getAvailableSingleFileImageFormats() + getAvailableMultipleImageFormats()

        SupportedImageFormat imageFormat =  imageFormats.find {
            it.mimeType == mimeType
        }

        imageFormat.absoluteFilePath = fif
        return imageFormat

    }

    static public Format getImageFormat(String uploadedFile) {
        def imageFormats = getAvailableSingleFileImageFormats() + getAvailableMultipleImageFormats()

        //hack to avoid running detect on ndpi
        //running detect on ndpi trigger a identity process that takes all memory for a big image
        try {
            int dot = uploadedFile.lastIndexOf('.');
            if(uploadedFile.substring(dot + 1).toLowerCase().equals("ndpi")) {
                HamamatsuNDPIFormat f = new HamamatsuNDPIFormat()
                f.absoluteFilePath = uploadedFile
                return f
            }
        } catch(Exception e) {
            println e
        }



        imageFormats.each {
            it.absoluteFilePath = uploadedFile
        }

        def result = imageFormats.find {
            it.detect()
        }

        if(!result) throw new FormatException("Undetected Format")
        return result
    }
}