package be.cytomine.formats

/*
 * Copyright (c) 2009-2015. Authors: see NOTICE file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import be.cytomine.formats.archive.ZipFormat
import be.cytomine.formats.digitalpathology.AperioSVSFormat
import be.cytomine.formats.digitalpathology.HamamatsuNDPIFormat
import be.cytomine.formats.digitalpathology.HamamatsuVMSFormat
import be.cytomine.formats.digitalpathology.LeicaSCNFormat
import be.cytomine.formats.digitalpathology.PhilipsTIFFFormat
import be.cytomine.formats.digitalpathology.SakuraSVSlideFormat
import be.cytomine.formats.digitalpathology.MiraxMRXSFormat
import be.cytomine.formats.specialtiff.CZITIFFFormat
import be.cytomine.formats.specialtiff.HuronTIFFFormat
import be.cytomine.formats.standard.BMPFormat
import be.cytomine.formats.standard.JPEG2000Format
import be.cytomine.formats.standard.JPEGFormat
import be.cytomine.formats.standard.PGMFormat
import be.cytomine.formats.standard.PNGFormat
import be.cytomine.formats.specialtiff.PhotoshopTIFFFormat
import be.cytomine.formats.standard.PlanarTIFFFormat
import be.cytomine.formats.standard.PyramidalTIFFFormat
import be.cytomine.formats.digitalpathology.VentanaTIFFFormat
import utils.FilesUtils

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
                //common formats
                new PhotoshopTIFFFormat(),
                new HuronTIFFFormat(),
                new PlanarTIFFFormat(),
                new PyramidalTIFFFormat(),
                new VentanaTIFFFormat(),
                new JPEG2000Format(),
                new JPEGFormat(),
                new PGMFormat(),
                new PNGFormat(),
                new BMPFormat()
                //new OMEXML(), //WORK IN PROGRESS
                //new ZeissZVI() //WORK IN PROGRESS
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
                    ImageFormat imageFormat = getImageFormat(extractedFile)
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

    static public ImageFormat getImageFormatByMimeType(String fif, String mimeType) {
        def imageFormats = getAvailableSingleFileImageFormats() + getAvailableMultipleImageFormats()

        ImageFormat imageFormat =  imageFormats.find {
            it.mimeType == mimeType
        }

        imageFormat.absoluteFilePath = fif
        return imageFormat

    }

    static public ImageFormat getImageFormat(String uploadedFile) {
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