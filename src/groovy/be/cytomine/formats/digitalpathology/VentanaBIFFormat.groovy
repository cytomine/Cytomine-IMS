package be.cytomine.formats.digitalpathology

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
    }

}
