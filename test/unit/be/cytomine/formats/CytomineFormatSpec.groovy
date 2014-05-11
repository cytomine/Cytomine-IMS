package be.cytomine.formats

import be.cytomine.ImageController
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


/**
 * Created by stevben on 23/04/14.
 */

@TestFor(ImageController)
class CytomineFormatSpec {

    private static String createFullPathFromFilename(def imageFilename, String imageRepository = '/opt/cytomine/testdata') {
        String uploadedFile = [imageRepository, imageFilename].join(File.separator)
        println uploadedFile
        assert new File(uploadedFile).exists()
        return uploadedFile
    }

    private static UploadedFile createUploadedFileFromImagePath(def imageFilename, def imageRepository = '/opt/cytomine/testdata') {
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

    void "test zipFormat"() {
        def uploadedFile = createFullPathFromFilename("499-488.zip")
        checkCorrectDetect(uploadedFile, ZipFormat.class)
    }

    void "test zipFormatMultipleSingleImages"() {
        String uploadedFile = createUploadedFileFromImagePath("499-488.zip")
        ImageFormat[] imageFormats = FormatIdentifier.getImageFormats(uploadedFile)
        assert(imageFormats.size() == 2)
        imageFormats.each { imageFormat ->
            assert(imageFormat.class == JPEGFormat)
        }
    }

    void "test zipMRXSDetect"() {
        String uploadedFile = createUploadedFileFromImagePath("CMU-1-Saved-1_16.mrxs.zip")
        ImageFormat[] imageFormats = FormatIdentifier.getImageFormats(uploadedFile)
        assert imageFormats.size() == 1
        assert imageFormats[0].class == MiraxMRXSFormat
    }

    void "test zipVMSDetect"() {
        String uploadedFile = createUploadedFileFromImagePath("CMU-1.vms.zip")
        ImageFormat[] imageFormats = FormatIdentifier.getImageFormats(uploadedFile)
        assert imageFormats.size() == 1
        assert imageFormats[0].class == HamamatsuVMSFormat
    }

    void "test convertJPEG"() {
        JPEGFormat jpegFormat = new JPEGFormat(absoluteFilePath: createFullPathFromFilename("384.jpg"))
        String convertedFilename = jpegFormat.convert("/tmp")
        assert(convertedFilename)
        println convertedFilename
        String uploadedFile = createUploadedFileFromImagePath(convertedFilename, "/")
        ImageFormat[] imageFormats = FormatIdentifier.getImageFormats(uploadedFile)
        assert imageFormats.size() == 1
        assert imageFormats[0].class == PyramidalTIFFFormat
    }
}