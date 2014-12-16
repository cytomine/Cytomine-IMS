package be.cytomine.formats.digitalpathology

import org.openslide.OpenSlide
import utils.FilesUtils

/**
 * Created by stevben on 22/04/14.
 */
class HamamatsuVMSFormat extends OpenSlideMultipleFileFormat {

    public HamamatsuVMSFormat() {
        extensions = ["vms"]
        vendor = "hamamatsu"
        mimeType = "openslide/vms"
        widthProperty = "openslide.level[0].width"
        heightProperty = "openslide.level[0].height"
        resolutionProperty = null //to compute
        magnificiationProperty = "hamamatsu.SourceLens"
    }

    boolean detect() {
        try {
            if(FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase().equals("tif")) return false //hack: if convert ndpi to tif => still hamamatsu metadata
            return new OpenSlide(new File(absoluteFilePath)).properties.keySet().contains("hamamatsu.MapFile")
        } catch (java.io.IOException e) {
            //Not a file that OpenSlide can recognize
            return false
        }
    }

    def properties() {
        def properties = super.properties()

        float physicalWidthProperty = Float.parseFloat(properties.find { it.key == "hamamatsu.PhysicalWidth"}.value)
        float widthProperty = (float) properties.find { it.key == "cytomine.width"}.value
        if (physicalWidthProperty && widthProperty) {
            def resolution = physicalWidthProperty / widthProperty / 1000
            properties << [ key : "cytomine.resolution", value : resolution]
        }
    }
}
