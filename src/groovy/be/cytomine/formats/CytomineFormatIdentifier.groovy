package be.cytomine.formats

import be.cytomine.client.models.UploadedFile
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
import be.cytomine.formats.standard.TIFFFormat
import be.cytomine.formats.standard.VentanaTIFFFormat

/**
 * Created by stevben on 22/04/14.
 */
public class CytomineFormatIdentifier {

    static public getAvailableImageFormats() {
        //check the extension and or content in order to identify the right CytomineFormat
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

    static public CytomineFormat getFormat(UploadedFile uploadedFile) {

        def imageFormats = getAvailableImageFormats()

        imageFormats.each {
            it.uploadedFile = uploadedFile
        }

        CytomineFormat detectedFormat = imageFormats.find {
            it.detect()
        }

        return detectedFormat
    }
}