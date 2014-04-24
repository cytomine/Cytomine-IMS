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
import be.cytomine.formats.standard.TIFFFormat

/**
 * Created by stevben on 22/04/14.
 */
public class CytomineFormatIdentifier {

    public UploadedFile uploadedFile

    public CytomineFormat getFormat() {
        //check the extension and or content in order to identify the right CytomineFormat
        def imageFormats = [
                //openslide compatibles formats
                new MiraxMRXSFormat(),
                new AperioSVSFormat(),
                new HamamatsuNDPIFormat(),
                new HamamatsuVMSFormat(),
                new LeicaSCNFormat(),
                new SakuraSVSlideFormat(),
                //common formats
                new TIFFFormat(),
                new JPEG2000Format(),
                new JPEGFormat(),
                new PGMFormat(),
                new PNGFormat(),
                new BMPFormat()
        ]

        imageFormats.each {
            it.uploadedFile = uploadedFile
        }

        CytomineFormat detectedFormat = imageFormats.find {
            it.detect()
        }

        println "Format detected : $detectedFormat.extension"

        return detectedFormat
    }
}