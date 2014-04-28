package be.cytomine.formats.digitalpathology

import be.cytomine.formats.CytomineFormat
import org.openslide.OpenSlide

/**
 * Created by stevben on 22/04/14.
 */
abstract class OpenSlideFormat extends CytomineFormat {

    protected String vendor = null

    boolean detect() {
        String imageAbsolutePath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)
        File slideFile = new File(imageAbsolutePath)
        if (slideFile.canRead()) {
            try {
            return OpenSlide.detectVendor(slideFile) == vendor
            } catch (java.io.IOException e) {
                //Not a file that OpenSlide can recognize
                return false
            }
        } else {
            //throw ERROR reading file
        }

    }

}
