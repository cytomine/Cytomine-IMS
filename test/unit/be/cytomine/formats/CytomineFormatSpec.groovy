package be.cytomine.formats

import be.cytomine.formats.digitalpathology.PhilipsTIFFFormat
import be.cytomine.formats.specialtiff.CZITIFFFormat
import be.cytomine.formats.specialtiff.PhotoshopTIFFFormat
import be.cytomine.image.ImageUtilsController


import be.cytomine.client.models.UploadedFile
import be.cytomine.formats.archive.ZipFormat
import be.cytomine.formats.digitalpathology.AperioSVSFormat
import be.cytomine.formats.digitalpathology.HamamatsuNDPIFormat
import be.cytomine.formats.digitalpathology.HamamatsuVMSFormat
import be.cytomine.formats.digitalpathology.LeicaSCNFormat
import be.cytomine.formats.digitalpathology.MiraxMRXSFormat
import be.cytomine.formats.standard.JPEGFormat
import be.cytomine.formats.standard.PNGFormat
import be.cytomine.formats.standard.PlanarTIFFFormat
import be.cytomine.formats.digitalpathology.VentanaTIFFFormat
import be.cytomine.formats.standard.PyramidalTIFFFormat
import grails.test.mixin.TestFor
import utils.ProcUtils


/**
 * Created by stevben on 23/04/14.
 */

@TestFor(ImageUtilsController)
class CytomineFormatSpec {

    final static String IMAGES_REPOSITORY_PATH = '/opt/cytomine/testdata'

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
        def imageFormats = FormatIdentifier.getAvailableSingleFileImageFormats()
        imageFormats.each {
            it.absoluteFilePath = uploadedFile
            if (it.class ==  expectedClass) {
                assert(it.detect())
            } else {
                assert(!it.detect())
            }
        }
    }

    void "test jpegdetect"() {
        def uploadedFile = createFullPathFromFilename("384.jpg")
        checkCorrectDetect(uploadedFile, JPEGFormat.class)
    }


    void "test jpegformat"() {
        def uploadedFile = createFullPathFromFilename("384.jpg")
        checkCorrectDetect(uploadedFile, JPEGFormat.class)
    }

    void "test pngformat"() {
        def uploadedFile = createFullPathFromFilename("384.png")
        checkCorrectDetect(uploadedFile, PNGFormat.class)
    }

    void "test ndpiformat"() {
        def uploadedFile = createFullPathFromFilename("CMU-1.ndpi")
        checkCorrectDetect(uploadedFile, HamamatsuNDPIFormat.class)
    }

    void "test vmsformat"() {
        def uploadedFile = createFullPathFromFilename("CMU-1.vms/CMU-1-40x_-_2010-01-12_13.24.05.vms")
        checkCorrectDetect(uploadedFile, HamamatsuVMSFormat.class)
    }

    void "test tiffplanarformat"() {
        def uploadedFile = createFullPathFromFilename("384.tiff")
        checkCorrectDetect(uploadedFile, PlanarTIFFFormat.class)
    }

    void "test tiffphotoshopformat"() {
        def uploadedFile = createFullPathFromFilename("1464-001-L.tif")
        checkCorrectDetect(uploadedFile, PhotoshopTIFFFormat.class)
    }

    void "test ventanatiffformat"() {
        def uploadedFile = createFullPathFromFilename("bif_tif/11GH076256_A2_CD3_100.tif")
        checkCorrectDetect(uploadedFile, VentanaTIFFFormat.class)
    }

    void "test aperioSVSFormat"() {
        def uploadedFile = createFullPathFromFilename("alphaSMA_B-1609444_3-2013-02-27-17.12.08.svs")
        checkCorrectDetect(uploadedFile, AperioSVSFormat.class)
    }

    void "test aperioSVSJ2KFormat"() {
        def uploadedFile = createFullPathFromFilename("JP2K-33003-1.svs")
        checkCorrectDetect(uploadedFile, AperioSVSFormat.class)
    }

    void "test leicaSCNFormat"() {
        def uploadedFile = createFullPathFromFilename("Leica-1.scn")
        checkCorrectDetect(uploadedFile, LeicaSCNFormat.class)
    }

    void "test miraxMRXSFormat"() {
        def uploadedFile = createFullPathFromFilename("CMU-1-Saved-1_16.mrxs/CMU-1-Saved-1_16.mrxs")
        checkCorrectDetect(uploadedFile, MiraxMRXSFormat.class)
    }

    void "test philipsTIFFFormat"() {
        def uploadedFile = createFullPathFromFilename("PhilipsTest02.tiff")
        checkCorrectDetect(uploadedFile, PhilipsTIFFFormat.class)
    }

    void "test zipFormat"() {
        def uploadedFile = getFilenameForTest("499-488.zip")
        checkCorrectDetect(uploadedFile, ZipFormat.class)
    }

    void "test zipFormatMultipleSingleImages"() {
        String uploadedFile = getFilenameForTest("499-488.zip")
        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
        assert(detectedFiles.size() == 2)
        detectedFiles.each { it ->
            assert(it.imageFormat.class == JPEGFormat)
        }
    }

    void "test zipMRXSDetect"() {
        String uploadedFile = getFilenameForTest("CMU-1-Saved-1_16.mrxs.zip")
        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
        assert detectedFiles.size() == 1
        assert detectedFiles[0].imageFormat.class == MiraxMRXSFormat
        assert(new File(uploadedFile).delete()) //clean tmp file (only mrxs file is cleaned, not nested...)
    }

    void "test zipVMSDetect"() {
        String uploadedFile = getFilenameForTest("CMU-1.vms.zip")
        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
        assert detectedFiles.size() == 1
        assert detectedFiles[0].imageFormat.class == HamamatsuVMSFormat //clean tmp file (only vms file is cleaned, not nested...)
    }

    void "test CZITIFFDetect"() {
        String uploadedFile = getFilenameForTest("CZITIFF_TEST.tif")
        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)

        assert detectedFiles.size() == 1
        assert detectedFiles[0].imageFormat.class == CZITIFFFormat
    }

    void "test convertJPEG"() {
        JPEGFormat jpegFormat = new JPEGFormat(absoluteFilePath: getFilenameForTest("384.jpg"))
        String convertedFilename = jpegFormat.convert("/tmp")
        assert(convertedFilename)
        println convertedFilename
        String uploadedFile = getFilenameForTest(convertedFilename, "/")
        def detectedFiles = FormatIdentifier.getImageFormats(uploadedFile)
        assert detectedFiles.size() == 1
        assert detectedFiles[0].imageFormat.class == PyramidalTIFFFormat
        assert(new File(uploadedFile).delete()) //clean tmp file
        assert(new File(convertedFilename).delete()) //clean tmp file
    }
}