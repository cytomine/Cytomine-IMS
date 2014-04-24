package be.cytomine.formats.digitalpathology

/**
 * Created by stevben on 22/04/14.
 */
class AperioSVSFormat extends OpenSlideCompatibleSingleFileFormat {

    public AperioSVSFormat(){
        extensions = ["svs"]
        vendor = "aperio"
    }
}
