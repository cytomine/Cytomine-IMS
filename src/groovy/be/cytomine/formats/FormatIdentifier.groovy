package be.cytomine.formats

import be.cytomine.formats.archive.ZipFormat
import be.cytomine.formats.heavyconvertable.OMETIFFFormat
import be.cytomine.formats.lightconvertable.BMPFormat
import be.cytomine.formats.heavyconvertable.CellSensVSIFormat
import be.cytomine.formats.lightconvertable.DICOMFormat
import be.cytomine.formats.heavyconvertable.DotSlideFormat
import be.cytomine.formats.lightconvertable.VentanaTIFFFormat
import be.cytomine.formats.lightconvertable.specialtiff.PlanarTIFFFormat
import be.cytomine.formats.supported.JPEG2000Format
import be.cytomine.formats.supported.PhilipsTIFFFormat
import be.cytomine.formats.supported.PyramidalTIFFFormat
import be.cytomine.formats.supported.VentanaBIFFormat
import be.cytomine.formats.supported.digitalpathology.*
import be.cytomine.formats.lightconvertable.JPEGFormat
import be.cytomine.formats.lightconvertable.PGMFormat
import be.cytomine.formats.lightconvertable.PNGFormat
import be.cytomine.formats.lightconvertable.specialtiff.BrokenTIFFFormat
import be.cytomine.formats.lightconvertable.specialtiff.CZITIFFFormat
import be.cytomine.formats.lightconvertable.specialtiff.HuronTIFFFormat
import be.cytomine.formats.lightconvertable.specialtiff.PhotoshopTIFFFormat
import be.cytomine.formats.supported.SupportedImageFormat
import utils.FilesUtils

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
                //openslide compatibles formats
                new AperioSVSFormat(),
                new HamamatsuNDPIFormat(),
                new LeicaSCNFormat(),
                new SakuraSVSlideFormat(),
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

    static public def getImageFormats(String uploadedFilePath) {

        def archiveFormats = getAvailableArchiveFormats()

        archiveFormats.each {
            it.absoluteFilePath = uploadedFilePath
        }

        ArchiveFormat detectedArchiveFormat = archiveFormats.find {
            it.detect()
        }

        def imageFormats = []
        if (detectedArchiveFormat) { //archive, we need to extract and analyze the content
            def extractedFiles = detectedArchiveFormat.extract(new File(uploadedFilePath).getParent())

            def hierarchicalMultipleFileImageFormats = getAvailableHierarchicalMultipleImageFormats()
            def extractedFolder = new File(uploadedFilePath).getParentFile().absolutePath

            // if the zip contained only one folder, we go into this folder
            def folders = new File(uploadedFilePath).getParentFile().listFiles(new FileFilter() {
                @Override
                boolean accept(File pathname) {
                    // substract the folder "MACOSX"
                    if(pathname.isDirectory() && pathname.name == "__MACOSX") return false;
                    //substract the original zip
                    if(pathname.absolutePath == uploadedFilePath) return false;
                    return true
                }
            });

            if (folders.size() == 1 && folders[0].isDirectory()) {
                File subFolder = folders[0]
                extractedFolder = subFolder.absolutePath
            }

            hierarchicalMultipleFileImageFormats.each { imageFormat ->
                imageFormat.absoluteFilePath = extractedFolder
                if (imageFormat.detect()) {
                    imageFormats << [
                            absoluteFilePath : imageFormat.absoluteFilePath,
                            imageFormat : imageFormat]
                    // Currently, we can't continue the process. Subimages cannot be processed independently
                    return imageFormats
                }
            }

            //multiple single image or a single image composed of multiple files ?
            //if (extractedFiles.size() > 1) {
            def multipleFileImageFormats = getAvailableMultipleImageFormats()

            //look for multiple files image formats (e.g mrxs & vms)
            extractedFiles.each {  extractedFile ->
                String ext = FilesUtils.getExtensionFromFilename(extractedFile).toLowerCase()
                multipleFileImageFormats.each { imageFormat ->
                    if (imageFormat.extensions.contains(ext)) {
                        imageFormat.absoluteFilePath = extractedFile
                        if (imageFormat.detect()) imageFormats << [
                                absoluteFilePath : imageFormat.absoluteFilePath,
                                imageFormat : imageFormat]
                    }
                }
            }

            //multiple single files (jpeg1, jpeg2, ...) ?
            if (imageFormats.size() == 0) { //obviously, we did not detect multiple files image formats
                extractedFiles.each {  extractedFile ->
                    Format imageFormat = getImageFormat(extractedFile)
                    if (imageFormat) imageFormats << [
                            absoluteFilePath : imageFormat.absoluteFilePath,
                            imageFormat : imageFormat]
                }
            }

        } else {
            imageFormats << [
                    uploadedFilePath : uploadedFilePath,
                    imageFormat : getImageFormat(uploadedFilePath)]
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

        return imageFormats.find {
            it.detect()
        }

    }
}