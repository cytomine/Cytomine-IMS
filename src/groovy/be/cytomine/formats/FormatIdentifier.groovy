package be.cytomine.formats

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

import be.cytomine.exception.FormatException
import be.cytomine.formats.archive.ArchiveFormat
import be.cytomine.formats.archive.ZipFormat
import be.cytomine.formats.heavyconvertable.CellSensVSIFormat
import be.cytomine.formats.heavyconvertable.DotSlideFormat
import be.cytomine.formats.heavyconvertable.OMETIFFFormat
import be.cytomine.formats.heavyconvertable.PerkinElmerVectraQPTIFF
import be.cytomine.formats.heavyconvertable.ZeissCZIFormat
import be.cytomine.formats.heavyconvertable.video.MP4Format
import be.cytomine.formats.lightconvertable.*
import be.cytomine.formats.lightconvertable.geospatial.GeoJPEG2000Format
import be.cytomine.formats.lightconvertable.geospatial.GeoTIFFFormat
import be.cytomine.formats.lightconvertable.specialtiff.*
import be.cytomine.formats.supported.JPEG2000Format
import be.cytomine.formats.supported.NativeFormat
import be.cytomine.formats.supported.PyramidalTIFFFormat
import be.cytomine.formats.supported.digitalpathology.*
import be.cytomine.formats.tools.CytomineFile
import be.cytomine.formats.tools.MultipleFilesFormat
import groovy.util.logging.Log4j

@Log4j
class FormatIdentifier {

    private def formats
    private CytomineFile file

    FormatIdentifier(CytomineFile file) {
        initializeFormats()
        setFile(file)
    }

    def setFile(file) {
        this.file = file

        if (!this.file.exists())
            throw new FileNotFoundException("File not found.")

        this.formats.each {
            it.file = this.file
        }
    }

    def initializeFormats() {
        this.formats = [
                // Fast detections
                new ZipFormat(), // detector: extension
                new JPEGFormat(), // detector: image magick
                new PGMFormat(), // detector: image magick
                new PNGFormat(), // detector: image magick
                new BMPFormat(), // detector: image magick
                new DICOMFormat(), // detector: image magick
                new PhotoshopTIFFFormat(), // detector: tiffinfo
                new HuronTIFFFormat(), // detector: tiffinfo
                new CZITIFFFormat(), // detector: tiffinfo
                new OMETIFFFormat(), // detector: tiffinfo
                new GeoTIFFFormat(), // detector: tiffinfo


                // Slow detections that must come before others
                new HamamatsuNDPIFormat(), // detector: openslide
                new VentanaTIFFFormat(), // detector: openslide
                new PhilipsTIFFFormat(), // detector: openslide
                new GeoJPEG2000Format(), // detector: extension + gdal
                

                // Fast detections that must go last (large detection criteria)
                new JPEG2000Format(), // Accept any JPEG2000
                new PyramidalTIFFFormat(), // Accept any pyramidal TIFF
                new PlanarTIFFFormat(), // Accept any planar TIFF
                new BrokenTIFFFormat(), // Accept any TIFF

                // Slow detections
                new AperioSVSFormat(), // detector: openslide
                new HamamatsuVMSFormat(), // detector: openslide
                new LeicaSCNFormat(), // detector: openslide
                new MiraxMRXSFormat(), // detector: openslide
                new SakuraSVSlideFormat(), // detector: openslide
                new VentanaBIFFormat(), // detector: openslide
                new CellSensVSIFormat(), // detector: extension
                new DotSlideFormat(), // detector: extension
                new ZeissCZIFormat(), // detector: custom
                new MP4Format(), // detector: ffprobe

        ]
    }

    def getArchiveFormats() {
        return this.formats.findAll { it instanceof ArchiveFormat }
    }

    def getNativeFormats() {
        return this.formats.findAll { it instanceof NativeFormat }
    }

    def getMultipleFilesFormats() {
        return this.formats.findAll { it instanceof MultipleFilesFormat }
    }

    def getSingleFileFormats() {
        return this.formats.findAll { !(it instanceof MultipleFilesFormat) }
    }

    Format identify() {
        def formatsToTest = (file.isDirectory()) ? getMultipleFilesFormats() : getSingleFileFormats()

        Format detected = formatsToTest.find {
            it.detect()
        }

        if (!detected)
            throw new FormatException("Format not found.")

        log.info("Detected format for $file is $detected")
        return detected
    }

    def identify(String mimeType, def onlyNative = false) {
        def formatsToTest = (onlyNative) ? getNativeFormats() : this.formats

        Format detected = formatsToTest.find {
            it.mimeType == mimeType
        }

        if (!detected)
            throw new FormatException("Format not found.")

        log.info("Detected format for $file is $detected")
        return detected
    }

    boolean isClassicFolder() {
        if (!file.isDirectory())
            return false

        def detected
        try {
            detected = identify()
        } catch (FormatException ignored) {
        }

        return detected == null
    }


    static getAvailableArchiveFormats() {
        return [
                new ZipFormat()
        ]
    }

    static getSupportedImageFormats() {
        return [
                new JPEG2000Format(),
                new PyramidalTIFFFormat(),
                new AperioSVSFormat(),
                new HamamatsuNDPIFormat(),
                new HamamatsuVMSFormat(),
                new LeicaSCNFormat(),
                new MiraxMRXSFormat(),
                new SakuraSVSlideFormat(),
                new PhilipsTIFFFormat(),
                new VentanaBIFFormat(),
                new VentanaTIFFFormat()
        ]
    }

    static NativeFormat getSupportedImageFormatByMimeType(String fif, String mimeType) {
        def imageFormats = getSupportedImageFormats()

        NativeFormat imageFormat = imageFormats.find {
            it.mimeType == mimeType
        }

        imageFormat.setFile(new CytomineFile(fif))
        return imageFormat

    }
}