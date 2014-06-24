package be.cytomine.formats.digitalpathology

import java.awt.image.BufferedImage

/**
 * Created by stevben on 22/04/14.
 */
class LeicaSCNFormat  extends OpenSlideSingleFileFormat {

    public LeicaSCNFormat() {
        extensions = ["scn"]
        vendor = "leica"
        mimeType = "openslide/scn"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = "openslide.mpp-x"
        magnificiationProperty = "leica.objective"
    }


    BufferedImage associated(String label) {
        BufferedImage bufferedImage = super.associated(label)
        if (label == "macro")
            return rotate90ToRight(bufferedImage)
        else
            return bufferedImage
    }
}
