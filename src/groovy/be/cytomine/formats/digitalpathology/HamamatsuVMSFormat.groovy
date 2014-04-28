package be.cytomine.formats.digitalpathology

import org.openslide.OpenSlide

/**
 * Created by stevben on 22/04/14.
 */
class HamamatsuVMSFormat extends OpenSlideCompatibleMultipleFileFormat {

    public HamamatsuVMSFormat() {
        extensions = ["vms"]
        vendor = "hamamatsu"
    }

    boolean detect() {
        //if (!super.detect()) return false //not an hamamatsu format
        try {
            return new OpenSlide(new File(uploadedFile.getAbsolutePath())).properties.keySet().contains("hamamatsu.MapFile")
        } catch (java.io.IOException e) {
            //Not a file that OpenSlide can recognize
            return false
        }

    }
}
