package be.cytomine.formats.digitalpathology

import org.openslide.OpenSlide

/**
 * Created by stevben on 22/04/14.
 */
class HamamatsuNDPIFormat extends OpenSlideCompatibleSingleFileFormat {

    public HamamatsuNDPIFormat() {
        extensions = ["ndpi"]
        vendor = "hamamatsu"
    }

    boolean detect() {
        if (!super.detect()) return false //not an hamamatsu format

        return !new OpenSlide(new File(uploadedFile.getAbsolutePath())).properties.keySet().contains("hamamatsu.MapFile")



    }
}
