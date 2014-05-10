package be.cytomine.formats

import be.cytomine.client.models.UploadedFile
import be.cytomine.formats.archive.ZipFormat
import be.cytomine.formats.digitalpathology.AperioSVSFormat
import be.cytomine.formats.digitalpathology.HamamatsuNDPIFormat
import be.cytomine.formats.digitalpathology.HamamatsuVMSFormat
import be.cytomine.formats.digitalpathology.LeicaSCNFormat
import be.cytomine.formats.digitalpathology.SakuraSVSlideFormat
import be.cytomine.formats.digitalpathology.MiraxMRXSFormat
import be.cytomine.formats.standard.BMPFormat
import be.cytomine.formats.standard.JPEG2000Format
import be.cytomine.formats.standard.JPEGFormat
import be.cytomine.formats.standard.PGMFormat
import be.cytomine.formats.standard.PNGFormat
import be.cytomine.formats.standard.PlanarTIFFFormat
import be.cytomine.formats.standard.PyramidalTIFFFormat
import be.cytomine.formats.digitalpathology.VentanaTIFFFormat

/**
 * Created by stevben on 22/04/14.
 */
public class FormatIdentifier {

    static public getAvailableArchiveFormats() {
        return [
                new ZipFormat()
        ]
    }

    static public getAvailableImageFormats() {
        //check the extension and or content in order to identify the right Format
        return [
                //openslide compatibles formats
                new MiraxMRXSFormat(),
                new AperioSVSFormat(),
                new HamamatsuNDPIFormat(),
                new HamamatsuVMSFormat(),
                new LeicaSCNFormat(),
                new SakuraSVSlideFormat(),
                //common formats
                new PlanarTIFFFormat(),
                new PyramidalTIFFFormat(),
                new VentanaTIFFFormat(),
                new JPEG2000Format(),
                new JPEGFormat(),
                new PGMFormat(),
                new PNGFormat(),
                new BMPFormat()
        ]
    }

    static public Format[] getFormat(UploadedFile uploadedFile) {

        String uploadedFilePath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)

        def archiveFormats = getAvailableArchiveFormats()

        archiveFormats.each {
            it.uploadedFilePath = uploadedFilePath
        }

        ArchiveFormat detectedArchiveFormat = archiveFormats.find {
            it.detect()
        }

        if (detectedArchiveFormat) { //extract
            /*def extractedFiles = detectedArchiveFormat.extract()
            def extractUploadedFiles = []
            extractedFiles.each {

            }*/
        } else {
            return getImageFormat(uploadedFile)
        }


    }

    static private ImageFormat getImageFormat(String uploadedFile) {
        def imageFormats = getAvailableImageFormats()

        imageFormats.each {
            it.uploadedFilePath = uploadedFile
        }

        ImageFormat detectedFormat = imageFormats.find {
            it.detect()
        }

        return detectedFormat
    }
}