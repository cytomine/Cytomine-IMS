package be.cytomine.formats.digitalpathology

/**
 * Created by stevben on 22/04/14.
 */
class LeicaSCNFormat  extends OpenSlideSingleFileFormat {

    public LeicaSCNFormat() {
        extensions = ["scn"]
        vendor = "leica"
        mimeType = "openslide/scn"
    }

}
