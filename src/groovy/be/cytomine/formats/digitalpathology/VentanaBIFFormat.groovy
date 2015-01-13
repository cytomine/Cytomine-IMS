package be.cytomine.formats.digitalpathology

import grails.util.Holders
import utils.ServerUtils

/**
 * Created by stevben on 19/06/14.
 */
class VentanaBIFFormat extends OpenSlideSingleFileFormat {

    public VentanaBIFFormat(){
        extensions = ["bif"]
        vendor = "ventana"
        mimeType = "ventana/bif"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = "ventana.ScanRes"
        magnificiationProperty = "ventana.Magnification"
        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerVentana)
    }

}
