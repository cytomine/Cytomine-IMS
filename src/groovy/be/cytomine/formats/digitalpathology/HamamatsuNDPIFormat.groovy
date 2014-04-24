package be.cytomine.formats.digitalpathology

/**
 * Created by stevben on 22/04/14.
 */
class HamamatsuNDPIFormat extends OpenSlideCompatibleSingleFileFormat {

    public HamamatsuNDPIFormat() {
        extensions = ["ndpi"]
        vendor = "hamamatsu"
    }
}
