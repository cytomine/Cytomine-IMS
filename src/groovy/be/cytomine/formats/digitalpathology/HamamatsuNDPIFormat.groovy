package be.cytomine.formats.digitalpathology

import org.openslide.OpenSlide
import utils.FilesUtils

/**
 * Created by stevben on 22/04/14.
 */
class HamamatsuNDPIFormat extends OpenSlideSingleFileFormat {

    public HamamatsuNDPIFormat() {
        extensions = ["ndpi"]
        vendor = "hamamatsu"
        mimeType = "openslide/ndpi"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = "openslide.mpp-x"
        magnificiationProperty = "hamamatsu.SourceLens"
    }

    boolean detect() {
        if (!super.detect()) return false //not an hamamatsu format
        if(FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase().equals("tif")) return false //hack: if convert ndpi to tif => still hamamatsu metadata
        return !new OpenSlide(new File(absoluteFilePath)).properties.keySet().contains("hamamatsu.MapFile")



    }
}
