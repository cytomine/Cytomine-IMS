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
            return OpenSlide.detectVendor(slideFile) == vendor
        } else {
            //throw ERROR reading file
        }

    }

}
