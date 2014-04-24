package be.cytomine.formats.digitalpathology

/**
 * Created by stevben on 22/04/14.
 */
class HamamatsuVMSFormat extends OpenSlideCompatibleMultipleFileFormat {

    public HamamatsuVMSFormat() {
        extensions = ["vms"]
        vendor = "hamamatsu"
    }
}
