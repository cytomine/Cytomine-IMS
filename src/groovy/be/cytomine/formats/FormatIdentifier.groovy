package be.cytomine.formats

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

    static public def getImageFormats(String uploadedFilePath) {

        def archiveFormats = getAvailableArchiveFormats()

        archiveFormats.each {
            it.absoluteFilePath = uploadedFilePath
        }

        ArchiveFormat detectedArchiveFormat = archiveFormats.find {
            it.detect()
        }

        if (detectedArchiveFormat) { //archive, we need to extract and analyze the content
            def extractedFiles = detectedArchiveFormat.extract(new File(uploadedFilePath).getParent())

            //multiple single image or a single image composed of multiple files ?
            //if (extractedFiles.size() > 1) {
            def multipleFileImageFormats = getAvailableMultipleImageFormats()
            def imageFormats = []

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
            return imageFormats

        } else {
            return [[uploadedFilePath : uploadedFilePath, imageFormat : getImageFormat(uploadedFilePath)]]
        }


    }

    static public ImageFormat getImageFormatByMimeType( String mimeType) {
        def imageFormats = getAvailableSingleFileImageFormats() + getAvailableMultipleImageFormats()

        return imageFormats.find {
            it.mimeType == mimeType
        }

    }

    static public ImageFormat getImageFormat(String uploadedFile) {
        def imageFormats = getAvailableSingleFileImageFormats() + getAvailableMultipleImageFormats()

        imageFormats.each {
            it.absoluteFilePath = uploadedFile
        }

        return imageFormats.find {
            it.detect()
        }

    }
}