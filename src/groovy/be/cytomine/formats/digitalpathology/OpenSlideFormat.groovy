package be.cytomine.formats.digitalpathology

import be.cytomine.formats.ImageFormat
import org.openslide.OpenSlide

/**
 * Created by stevben on 22/04/14.
 */
abstract class OpenSlideFormat extends ImageFormat {

    protected String vendor = null

    boolean detect() {
        File slideFile = new File(absoluteFilePath)
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

    String convert(String workingPath) {
        return null //nothing to do, the format is understood by IIP+OpenSlide natively
    }

}
