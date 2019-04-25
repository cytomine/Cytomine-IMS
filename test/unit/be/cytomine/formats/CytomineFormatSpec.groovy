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

import be.cytomine.formats.archive.ZipFormat
import be.cytomine.formats.heavyconvertable.CellSensVSIFormat
import be.cytomine.formats.heavyconvertable.DotSlideFormat
import be.cytomine.formats.heavyconvertable.OMETIFFFormat
import be.cytomine.formats.heavyconvertable.ZeissCZIFormat
import be.cytomine.formats.lightconvertable.*
import be.cytomine.formats.lightconvertable.geospatial.GeoJPEG2000Format
import be.cytomine.formats.lightconvertable.geospatial.GeoTIFFFormat
import be.cytomine.formats.lightconvertable.specialtiff.*
import be.cytomine.formats.supported.JPEG2000Format
import be.cytomine.formats.supported.PyramidalTIFFFormat
import be.cytomine.formats.supported.digitalpathology.*
import be.cytomine.formats.tools.CytomineFile
import be.cytomine.storage.StorageController
import grails.test.mixin.TestFor
import grails.util.Holders
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Specification

@TestFor(StorageController)
class CytomineFormatSpec extends Specification {

    final static String IMAGES_REPOSITORY_PATH = "/data/images/IMS-testset"

    private static CytomineFile createCytomineFileFromFilename(def imageFilename, String imageRepository = IMAGES_REPOSITORY_PATH) {
        CytomineFile uploadedFile = new CytomineFile([imageRepository, imageFilename].join(File.separator))
        println uploadedFile
        assert uploadedFile.exists()
        return uploadedFile
    }

    void "test detection JPEG format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("jpeg.jpeg")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof JPEGFormat
    }

    void "test conversion JPEG format"() {
        given:
        def file = createCytomineFileFromFilename("jpeg.jpeg")
        def format = new JPEGFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    void "test detection PNG 8bits format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("png-8.png")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof PNGFormat
    }

    void "test conversion PNG 8bits format"() {
        given:
        def file = createCytomineFileFromFilename("png-8.png")
        def format = new PNGFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    void "test detection PGM format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("pgm.pgm")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof PGMFormat
    }

    void "test conversion PGM format"() {
        given:
        def file = createCytomineFileFromFilename("pgm.pgm")
        def format = new PGMFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    void "test detection BMP format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("bmp.bmp")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof BMPFormat
    }

    void "test conversion BMP format"() {
        given:
        def file = createCytomineFileFromFilename("bmp.bmp")
        def format = new BMPFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    void "test detection DICOM 8bits format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("dicom-8.dcm")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof DICOMFormat
    }

    void "test conversion DICOM 8bits format"() {
        given:
        def file = createCytomineFileFromFilename("dicom-8.dcm")
        def format = new DICOMFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    @IgnoreIf({ !Holders.config.cytomine.Jpeg2000Enabled })
    void "test detection JPEG2000 format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("jp2.jp2")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof JPEG2000Format
    }

    void "test detection pyramidal TIFF format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("pyrtiff.tif")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof PyramidalTIFFFormat
    }

    void "test detection planar TIFF 8bits format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("tiff-8.tif")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof PlanarTIFFFormat
    }

    void "test conversion planar TIFF 8bits format"() {
        given:
        def file = createCytomineFileFromFilename("tiff-8.tif")
        def format = new PlanarTIFFFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    void "test detection planar TIFF 16bits format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("tiff-16.tif")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof PlanarTIFFFormat
    }

    void "test conversion planar TIFF 16bits format"() {
        given:
        def file = createCytomineFileFromFilename("tiff-16.tif")
        def format = new PlanarTIFFFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    @Ignore
    //TODO missing test sample
    void "test detection PhotoshopTIFF format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("photoshop.tif")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof PhotoshopTIFFFormat
    }

    @Ignore
    //TODO missing test sample
    void "test conversion PhotoshopTIFF format"() {
        given:
        def file = createCytomineFileFromFilename("photoshop.tif")
        def format = new PhotoshopTIFFFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    @Ignore
    //TODO missing test sample
    void "test detection HuronTIFF format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("huron.tif")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof HuronTIFFFormat
    }

    @Ignore
    //TODO missing test sample
    void "test conversion HuronTIFF format"() {
        given:
        def file = createCytomineFileFromFilename("huron.tif")
        def format = new HuronTIFFFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    @Ignore
    //TODO missing test sample
    void "test detection BrokenTIFF format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("broken.tif")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof BrokenTIFFFormat
    }

    @Ignore
    //TODO missing test sample
    void "test conversion BrokenTIFF format"() {
        given:
        def file = createCytomineFileFromFilename("broken.tif")
        def format = new BrokenTIFFFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    @Ignore
    //TODO missing test sample
    void "test detection CziTIFF format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("czi.tif")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof CZITIFFFormat
    }

    @Ignore
    //TODO missing test sample
    void "test conversion CziTIFF format"() {
        given:
        def file = createCytomineFileFromFilename("czi.tif")
        def format = new CZITIFFFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    void "test detection GeoTIFF format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("geotiff.tif")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof GeoTIFFFormat
    }

    void "test conversion GeoTIFF format"() {
        given:
        def file = createCytomineFileFromFilename("geotiff.tif")
        def format = new GeoTIFFFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    void "test detection GeoJPEG2000 format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("geojp2.jp2")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof GeoJPEG2000Format
    }

    void "test conversion GeoJPEG2000 format"() {
        given:
        def file = createCytomineFileFromFilename("geojp2.jp2")
        def format = new GeoJPEG2000Format()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof GeoTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    void "test detection Hamamatsu NDPI format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("ndpi.ndpi")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof HamamatsuNDPIFormat
    }

    void "test detection Aperio SVS Format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("svs.svs")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof AperioSVSFormat
    }

    void "test detection Ventana BIF format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("bif.bif")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof VentanaBIFFormat
    }

    void "test detection Leica SCN Format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("scn.scn")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof LeicaSCNFormat
    }

    void "test detection Mirax MRXS Format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("mrxs/")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof MiraxMRXSFormat
    }

    void "test detection Hamamatsu VMS format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("vms/")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof HamamatsuVMSFormat
    }

    @Ignore
    //TODO: sample test missing
    void "test detection Ventana tiff format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("ventana.tif")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof VentanaTIFFFormat
    }

    @Ignore
    //TODO: sample test missing
    void "test detection Philips TIFF Format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("philips.tif")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof PhilipsTIFFFormat
    }

    void "test detection CellSens VSI format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("vsi/")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof CellSensVSIFormat
    }

    @Ignore
    //TODO: Conversion too long -> find a smaller VSI for tests
    void "test conversion CellSens VSI format"() {
        given:
        def file = createCytomineFileFromFilename("vsi/")
        def format = new CellSensVSIFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat // TODO: PlanarTiff ?

        cleanup:
        converted.each { it.delete() }
    }

    void "test detection OME-TIFF format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("ometiff.ome.tif")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof OMETIFFFormat
    }

    void "test conversion OME-TIFF format"() {
        given:
        def file = createCytomineFileFromFilename("ometiff.ome.tif")
        def format = new OMETIFFFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() > 0
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    void "test detection Zeiss CZI Format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("czi-2-channel.czi")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof ZeissCZIFormat
    }

    void "test conversion Zeiss CZI format"() {
        given:
        def file = createCytomineFileFromFilename("czi-2-channel.czi")
        def format = new ZeissCZIFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() > 0
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat
        new FormatIdentifier(converted.get(1)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    @Ignore
    //TODO: missing test sample
    void "test detection Dotslide Format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("dotslide/")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof DotSlideFormat
    }

    @Ignore
    //TODO: missing test sample
    void "test conversion Dotslide format"() {
        given:
        def file = createCytomineFileFromFilename("dotslide/")
        def format = new DotSlideFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1
        new FormatIdentifier(converted.get(0)).identify() instanceof PyramidalTIFFFormat

        cleanup:
        converted.each { it.delete() }
    }

    void "test detection ZIP Format"() {
        given:
        def uploadedFile = createCytomineFileFromFilename("zip.zip")
        when:
        def format = new FormatIdentifier(uploadedFile).identify()
        then:
        format instanceof ZipFormat
    }

    void "test conversion ZIP format"() {
        given:
        def file = createCytomineFileFromFilename("zip.zip")
        def format = new ZipFormat()
        format.setFile(file)
        format.detect()

        when:
        def converted = format.convert()

        then:
        converted.size() == 1

        cleanup:
        converted.each { it.delete() }
    }

//    private static String getFilenameForTest(def imageFilename, String imageRepository = IMAGES_REPOSITORY_PATH) {
//        String source = createCytomineFileFromFilename(imageFilename, imageRepository)
//        String target = ["/tmp", new Date().getTime(), imageFilename].join(File.separator)
//        String targetDir = new File(target).getParent()
//        def command = "mkdir -p $targetDir"
//        ProcUtils.executeOnShell(command)
//        command = "cp -r $source $target"
//        ProcUtils.executeOnShell(command)
//        assert(new File(target).exists())
//        return target
//    }
//    void "test zipFormatMultipleSingleImages"() {
//        String uploadedFile = getFilenameForTest("zip/test.zip")
//        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
//        assert(detectedFiles.size() == 3)
//        assert (detectedFiles.collect {it.imageFormat.class}.sort() == [PhotoshopTIFFFormat, PhotoshopTIFFFormat, PNGFormat].sort())
//        //clean tmp file
//        FileUtils.deleteDirectory(new File(uploadedFile).parentFile.parentFile)
//    }
//
//    void "test zipMRXSDetect"() {
//        String uploadedFile = getFilenameForTest("mrxs/MRXS.zip")
//        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
//        assert detectedFiles.size() == 1
//        assert detectedFiles[0].imageFormat.class == MiraxMRXSFormat
//        assert(new File(uploadedFile).delete()) //clean tmp file (only mrxs file is cleaned, not nested...)
//        //clean tmp file
//        FileUtils.deleteDirectory(new File(uploadedFile).parentFile.parentFile)
//    }
//
//    void "test zipVMSDetect"() {
//        String uploadedFile = getFilenameForTest("vms/test.zip")
//        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
//        assert detectedFiles.size() == 1
//        assert detectedFiles[0].imageFormat.class == HamamatsuVMSFormat //clean tmp file (only vms file is cleaned, not nested...)
//        //clean tmp file
//        FileUtils.deleteDirectory(new File(uploadedFile).parentFile.parentFile)
//    }
//
//    void "test zipFormatSimpleMultipleAndZip"() {
//        String uploadedFile = getFilenameForTest("zip/testSimpleMultipleAndZip.zip")
//        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
//        assert(detectedFiles.size() == 6)
//        assert (detectedFiles.collect {it.imageFormat.class}.sort() == [PhotoshopTIFFFormat, PhotoshopTIFFFormat, PNGFormat, HamamatsuVMSFormat, MiraxMRXSFormat, PyramidalTIFFFormat].sort())
//        //clean tmp file
//        FileUtils.deleteDirectory(new File(uploadedFile).parentFile.parentFile)
//    }
//
//    void "test zipFormatTwoMultiples"() {
//        String uploadedFile = getFilenameForTest("zip/testVMSAndMRXS.zip")
//        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
//        assert(detectedFiles.size() == 2)
//        assert (detectedFiles.collect {it.imageFormat.class}.sort() == [HamamatsuVMSFormat, MiraxMRXSFormat].sort())
//        //clean tmp file
//        FileUtils.deleteDirectory(new File(uploadedFile).parentFile.parentFile)
//    }
//
//    void "test zipFormatInvalidTwoMultiples"() {
//        String uploadedFile = getFilenameForTest("zip/testInvalidVMSAndMRXS.zip")
//        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
//        assert(detectedFiles.size() == 1)
//        def clazz = detectedFiles.collect {it.imageFormat.class}[0]
//        assert (clazz == HamamatsuVMSFormat || clazz == MiraxMRXSFormat)
//        //clean tmp file
//        FileUtils.deleteDirectory(new File(uploadedFile).parentFile.parentFile)
//    }
}