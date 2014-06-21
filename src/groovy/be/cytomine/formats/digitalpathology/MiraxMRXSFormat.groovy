package be.cytomine.formats.digitalpathology

import java.awt.image.BufferedImage

/**
 * Created by stevben on 22/04/14.
 */
class MiraxMRXSFormat extends OpenSlideMultipleFileFormat {

    public MiraxMRXSFormat() {
        extensions = ["mrxs"]
        vendor = "mirax"
        mimeType = "openslide/mrxs"
    }

    BufferedImage associated(String label) {
        BufferedImage bufferedImage = super.associated(label)
        if (label == "macro")
            return rotate90ToRight(bufferedImage)
        else
            return bufferedImage
    }

}