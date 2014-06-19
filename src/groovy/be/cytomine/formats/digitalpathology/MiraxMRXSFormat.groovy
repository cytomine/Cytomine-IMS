package be.cytomine.formats.digitalpathology

/**
 * Created by stevben on 22/04/14.
 */
class MiraxMRXSFormat extends OpenSlideMultipleFileFormat {

    public MiraxMRXSFormat() {
        extensions = ["mrxs"]
        vendor = "mirax"
        mimeType = "openslide/mrxs"
    }

}