package be.cytomine.formats.digitalpathology

/**
 * Created by stevben on 22/04/14.
 */
class AperioSVSFormat extends OpenSlideSingleFileFormat {

    public AperioSVSFormat(){
        extensions = ["svs"]
        vendor = "aperio"
        mimeType = "openslide/svs"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = "aperio.MPP"
        magnificiationProperty = "aperio.AppMag"
    }
}
