package be.cytomine.formats.digitalpathology

import be.cytomine.formats.standard.TIFFFormat
import grails.util.Holders
import org.openslide.OpenSlide
import utils.ProcUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 28/04/14.
 */
class VentanaTIFFFormat extends OpenSlideSingleFileFormat {

    public VentanaTIFFFormat() {
        extensions = ["tif", "vtif"]
        vendor = "ventana"
        mimeType = "openslide/ventana"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = "openslide.mpp-x"
        magnificiationProperty = "openslide.objective-power"
    }
	
    String convert(String workingPath) {
        String source = absoluteFilePath
        String target = [new File(absoluteFilePath).getParent(), UUID.randomUUID().toString() + ".vtif"].join(File.separator)
		//make a symbolic link to the original file with a special extension 'vtif' in order to recognize the format within IIP.
        "ln -s $source $target".execute()
        return target
    }


    BufferedImage associated(String label) {
        BufferedImage bufferedImage = super.associated(label)
        if (label == "macro")
            return rotate90ToRight(bufferedImage)
        else
            return bufferedImage
    }
}
