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
import be.cytomine.client.models.UploadedFile
import be.cytomine.formats.archive.ZipFormat
import be.cytomine.formats.heavyconvertable.CellSensVSIFormat
import be.cytomine.formats.heavyconvertable.ZeissCZIFormat
import be.cytomine.formats.lightconvertable.specialtiff.CZITIFFFormat
import be.cytomine.formats.lightconvertable.specialtiff.PhotoshopTIFFFormat
import be.cytomine.formats.lightconvertable.JPEGFormat
import be.cytomine.formats.lightconvertable.PNGFormat
import be.cytomine.formats.lightconvertable.specialtiff.PlanarTIFFFormat
import be.cytomine.formats.lightconvertable.BMPFormat
import be.cytomine.formats.lightconvertable.PGMFormat
import be.cytomine.formats.lightconvertable.specialtiff.BrokenTIFFFormat
import be.cytomine.formats.lightconvertable.specialtiff.HuronTIFFFormat
import be.cytomine.formats.supported.digitalpathology.*
import be.cytomine.formats.supported.JPEG2000Format
import be.cytomine.formats.supported.PhilipsTIFFFormat
import be.cytomine.formats.supported.PyramidalTIFFFormat
import be.cytomine.formats.supported.VentanaBIFFormat
import be.cytomine.formats.supported.VentanaTIFFFormat
import be.cytomine.image.ImageResponseController
import grails.test.mixin.TestFor
import grails.util.Holders
import org.apache.commons.io.FileUtils
import utils.ProcUtils

/**
 * Created by stevben on 23/04/14.
 */

@TestFor(ImageResponseController)
class CytomineFormatSpec {

    final static String IMAGES_REPOSITORY_PATH = '/data/cytominetest/images'

    private static String getFilenameForTest(def imageFilename, String imageRepository = IMAGES_REPOSITORY_PATH) {
        String source = createFullPathFromFilename(imageFilename, imageRepository)
        String target = ["/tmp", new Date().getTime(), imageFilename].join(File.separator)
        String targetDir = new File(target).getParent()
        def command = "mkdir -p $targetDir"
        ProcUtils.executeOnShell(command)
        command = "cp -r $source $target"
        ProcUtils.executeOnShell(command)
        assert(new File(target).exists())
        return target
    }

    private static String createFullPathFromFilename(def imageFilename, String imageRepository = IMAGES_REPOSITORY_PATH) {
        String uploadedFile = [imageRepository, imageFilename].join(File.separator)
        println uploadedFile
        assert new File(uploadedFile).exists()
        return uploadedFile
    }

    private static UploadedFile createUploadedFileFromImagePath(def imageFilename, def imageRepository = IMAGES_REPOSITORY_PATH) {
        File file = new File([imageRepository, imageFilename].join(File.separator))
        UploadedFile uploadedFile = null
        if (file.canRead()) {
            uploadedFile = new UploadedFile()
            uploadedFile.set("path", imageRepository)
            uploadedFile.set("filename", imageFilename) //for test only
        }

        assert(file != null)

        return uploadedFile
    }


    private static checkCorrectDetect(String uploadedFile, Class expectedClass){
        def imageFormats = FormatIdentifier.getImageFormats(uploadedFile)
        println imageFormats

        def format = imageFormats.find { expectedClass.isInstance(it.imageFormat) }

        assert(format != null)
    }


    /*********************** Test formats ***********************/
    // Common Format
    void "test jpegformat"() {
        def uploadedFile = createFullPathFromFilename("jpeg/test.jpeg")
        checkCorrectDetect(uploadedFile, JPEGFormat.class)
    }

    void "test pngformat"() {
        def uploadedFile = createFullPathFromFilename("png/test.png")
        checkCorrectDetect(uploadedFile, PNGFormat.class)
    }

    void "test pgmformat"() {
        def uploadedFile = createFullPathFromFilename("pgm/test.pgm")
        checkCorrectDetect(uploadedFile, PGMFormat.class)
    }

    void "test bmpformat"() {
        def uploadedFile = createFullPathFromFilename("bmp/test.bmp")
        checkCorrectDetect(uploadedFile, BMPFormat.class)
    }


    // TIFF Format
    void "test tiffplanarformat"() {
        def uploadedFile = createFullPathFromFilename("tiff/planar.tif")
        checkCorrectDetect(uploadedFile, PlanarTIFFFormat.class)
    }

    void "test tiffphotoshopformat"() {
        def uploadedFile = createFullPathFromFilename("tiff/photoshop.tif")
        checkCorrectDetect(uploadedFile, PhotoshopTIFFFormat.class)
    }

    void "test ventanatiffformat"() {
        def uploadedFile = createFullPathFromFilename("tiff/ventana.tif")
        checkCorrectDetect(uploadedFile, VentanaTIFFFormat.class)
    }

    void "test huronTIFFformat"() {
        def uploadedFile = createFullPathFromFilename("tiff/huron.tif")
        checkCorrectDetect(uploadedFile, HuronTIFFFormat.class)
    }

    void "test brokenTIFFformat"() {
        def uploadedFile = createFullPathFromFilename("tiff/broken.tif")
        checkCorrectDetect(uploadedFile, BrokenTIFFFormat.class)
    }

    void "test pyrTIFFformat"() {
        def uploadedFile = createFullPathFromFilename("tiff/pyramidal.tif")
        checkCorrectDetect(uploadedFile, PyramidalTIFFFormat.class)
    }

    void "test philipsTIFFFormat"() {
        def uploadedFile = createFullPathFromFilename("tiff/philips.tif")
        checkCorrectDetect(uploadedFile, PhilipsTIFFFormat.class)
    }

    void "test CZITIFFDetect"() {
        def uploadedFile = createFullPathFromFilename("tiff/czi.tif")
        checkCorrectDetect(uploadedFile, CZITIFFFormat.class)
    }


    // Single File Format
    void "test ndpiformat"() {
        def uploadedFile = createFullPathFromFilename("ndpi/test.ndpi")
        checkCorrectDetect(uploadedFile, HamamatsuNDPIFormat.class)
    }

    void "test aperioSVSFormat"() {
        def uploadedFile = createFullPathFromFilename("svs/test.svs")
        checkCorrectDetect(uploadedFile, AperioSVSFormat.class)
    }

    void "test ventanabifformat"() {
        def uploadedFile = createFullPathFromFilename("bif/test.bif")
        checkCorrectDetect(uploadedFile, VentanaBIFFormat.class)
    }

    void "test J2Kformat"() {
        if(!Holders.config.cytomine.Jpeg2000Enabled) return;
        def uploadedFile = createFullPathFromFilename("j2k/test.jp2")
        checkCorrectDetect(uploadedFile, JPEG2000Format.class)
    }

    void "test leicaSCNFormat"() {
        def uploadedFile = createFullPathFromFilename("scn/test.scn")
        checkCorrectDetect(uploadedFile, LeicaSCNFormat.class)
    }

    // CZI Format
    void "test CZIFormat"() {
        def uploadedFile = createFullPathFromFilename("czi/test.czi")
        checkCorrectDetect(uploadedFile, ZeissCZIFormat.class)
    }
    void "test CZIFormat multiChan"() {
        def uploadedFile = createFullPathFromFilename("czi/test_multichan.czi")
        checkCorrectDetect(uploadedFile, ZeissCZIFormat.class)
    }


    // Multiple File Format
    void "test miraxMRXSFormat"() {
        def uploadedFile = createFullPathFromFilename("mrxs/")
        checkCorrectDetect(uploadedFile, MiraxMRXSFormat.class)
    }

    void "test vmsformat"() {
        def uploadedFile = createFullPathFromFilename("vms/")
        checkCorrectDetect(uploadedFile, HamamatsuVMSFormat.class)
    }

    void "test vsiformat"() {
        def uploadedFile = createFullPathFromFilename("vsi/")
        checkCorrectDetect(uploadedFile, CellSensVSIFormat.class)
    }


    // Zip Format
    void "test zipFormat"() {
        def uploadedFile = createFullPathFromFilename("mrxs/MRXS.zip")

        def format = FormatIdentifier.getAvailableArchiveFormats().find {
            it.absoluteFilePath = uploadedFile
            return (it.class ==  ZipFormat.class)
        }
        assert(format != null)
    }

    void "test zipFormatMultipleSingleImages"() {
        String uploadedFile = getFilenameForTest("zip/test.zip")
        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
        assert(detectedFiles.size() == 3)
        assert (detectedFiles.collect {it.imageFormat.class}.sort() == [PhotoshopTIFFFormat, PhotoshopTIFFFormat, PNGFormat].sort())
        //clean tmp file
        FileUtils.deleteDirectory(new File(uploadedFile).parentFile.parentFile)
    }

    void "test zipMRXSDetect"() {
        String uploadedFile = getFilenameForTest("mrxs/MRXS.zip")
        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
        assert detectedFiles.size() == 1
        assert detectedFiles[0].imageFormat.class == MiraxMRXSFormat
        assert(new File(uploadedFile).delete()) //clean tmp file (only mrxs file is cleaned, not nested...)
        //clean tmp file
        FileUtils.deleteDirectory(new File(uploadedFile).parentFile.parentFile)
    }

    void "test zipVMSDetect"() {
        String uploadedFile = getFilenameForTest("vms/test.zip")
        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
        assert detectedFiles.size() == 1
        assert detectedFiles[0].imageFormat.class == HamamatsuVMSFormat //clean tmp file (only vms file is cleaned, not nested...)
        //clean tmp file
        FileUtils.deleteDirectory(new File(uploadedFile).parentFile.parentFile)
    }

    void "test zipFormatSimpleMultipleAndZip"() {
        String uploadedFile = getFilenameForTest("zip/testSimpleMultipleAndZip.zip")
        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
        assert(detectedFiles.size() == 6)
        assert (detectedFiles.collect {it.imageFormat.class}.sort() == [PhotoshopTIFFFormat, PhotoshopTIFFFormat, PNGFormat, HamamatsuVMSFormat, MiraxMRXSFormat, PyramidalTIFFFormat].sort())
        //clean tmp file
        FileUtils.deleteDirectory(new File(uploadedFile).parentFile.parentFile)
    }

    void "test zipFormatTwoMultiples"() {
        String uploadedFile = getFilenameForTest("zip/testVMSAndMRXS.zip")
        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
        assert(detectedFiles.size() == 2)
        assert (detectedFiles.collect {it.imageFormat.class}.sort() == [HamamatsuVMSFormat, MiraxMRXSFormat].sort())
        //clean tmp file
        FileUtils.deleteDirectory(new File(uploadedFile).parentFile.parentFile)
    }

    void "test zipFormatInvalidTwoMultiples"() {
        String uploadedFile = getFilenameForTest("zip/testInvalidVMSAndMRXS.zip")
        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
        assert(detectedFiles.size() == 1)
        def clazz = detectedFiles.collect {it.imageFormat.class}[0]
        assert (clazz == HamamatsuVMSFormat || clazz == MiraxMRXSFormat)
        //clean tmp file
        FileUtils.deleteDirectory(new File(uploadedFile).parentFile.parentFile)
    }

    // TODO other convert

    void "test convertJPEG"() {
        String filePath = getFilenameForTest("jpeg/test.jpeg");
        JPEGFormat jpegFormat = new JPEGFormat(absoluteFilePath: filePath)
        String convertedFilename = jpegFormat.convert()
        assert(convertedFilename)
        def detectedFiles = FormatIdentifier.getImageFormats(convertedFilename)
        assert detectedFiles.size() == 1
        assert detectedFiles[0].imageFormat.class == PyramidalTIFFFormat
        assert(new File(convertedFilename).delete()) //clean tmp file
        //clean tmp file
        FileUtils.deleteDirectory(new File(filePath).parentFile.parentFile)
    }
}