package be.cytomine.formats

import be.cytomine.ImageController
import be.cytomine.client.models.UploadedFile
import be.cytomine.formats.digitalpathology.AperioSVSFormat
import be.cytomine.formats.digitalpathology.HamamatsuNDPIFormat
import be.cytomine.formats.digitalpathology.HamamatsuVMSFormat
import be.cytomine.formats.standard.JPEGFormat
import be.cytomine.formats.standard.PNGFormat
import be.cytomine.formats.standard.PlanarTIFFFormat
import be.cytomine.formats.standard.VentanaTIFFFormat
import grails.test.mixin.TestFor


/**
 * Created by stevben on 23/04/14.
 */

@TestFor(ImageController)
class CytomineFormatSpec {

    def grailsApplication

    private UploadedFile createUploadedFileFromImagePath(def imageFilename) {
        String imageRepository = '/Users/stevben/Cytomine/Slides'
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


    private checkCorrectDetect(UploadedFile uploadedFile, Class expectedClass){
        def imageFormats = CytomineFormatIdentifier.getAvailableImageFormats()
        imageFormats.each {
            it.uploadedFile = uploadedFile
            if (it.class ==  expectedClass) {
                assert(it.detect())
            } else {
                assert(!it.detect())
            }
        }
    }

    void "test jpegdetect"() {
        def uploadedFile = createUploadedFileFromImagePath("384.jpg")
        CytomineFormat cytomineFormat = CytomineFormatIdentifier.getFormat(uploadedFile)
        assert(cytomineFormat instanceof JPEGFormat)
    }


    void "test jpegformat"() {
        def uploadedFile = createUploadedFileFromImagePath("384.jpg")
        checkCorrectDetect(uploadedFile, JPEGFormat.class)
    }

    void "test pngformat"() {
        def uploadedFile = createUploadedFileFromImagePath("384.png")
        checkCorrectDetect(uploadedFile, PNGFormat.class)
    }

    void "test ndpiformat"() {
        def uploadedFile = createUploadedFileFromImagePath("CMU-1.ndpi")
        checkCorrectDetect(uploadedFile, HamamatsuNDPIFormat.class)
    }

    void "test vmsformat"() {
        def uploadedFile = createUploadedFileFromImagePath("CMU-1.vms/CMU-1-40x_-_2010-01-12_13.24.05.vms")
        checkCorrectDetect(uploadedFile, HamamatsuVMSFormat.class)
    }

    void "test tiffplanarformat"() {
        def uploadedFile = createUploadedFileFromImagePath("384.tiff")
        checkCorrectDetect(uploadedFile, PlanarTIFFFormat.class)
    }

    void "test ventanatiffformat"() {
        def uploadedFile = createUploadedFileFromImagePath("bif_tif/11GH076256_A2_CD3_100.tif")
        checkCorrectDetect(uploadedFile, VentanaTIFFFormat.class)
    }

    void "test aperioSVSFormat"() {
        def uploadedFile = createUploadedFileFromImagePath("alphaSMA_B-1609444_3-2013-02-27-17.12.08.svs")
        checkCorrectDetect(uploadedFile, AperioSVSFormat.class)
    }

    void "test aperioSVSJ2KFormat"() {
        def uploadedFile = createUploadedFileFromImagePath("JP2K-33003-1.svs")
        checkCorrectDetect(uploadedFile, AperioSVSFormat.class)
    }
}