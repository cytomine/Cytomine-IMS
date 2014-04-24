package be.cytomine.formats.digitalpathology

/**
 * Created by stevben on 22/04/14.
 */
class MiraxMRXSFormat extends OpenSlideCompatibleMultipleFileFormat {

    private final String EXTENSION = "mrxs"
    private final String VENDOR = "mirax"

    public String getExtension() {
        return EXTENSION
    }

    protected String getVendor() {
        return VENDOR
    }
}