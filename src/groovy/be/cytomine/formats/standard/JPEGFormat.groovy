package be.cytomine.formats.standard

import org.openslide.OpenSlide

/**
 * Created by stevben on 22/04/14.
 */
class JPEGFormat extends CommonFormat {

    public JPEGFormat () {
        extensions = ["jpg", "jpeg"]
        IMAGE_MAGICK_FORMAT_IDENTIFIER = "Format: JPEG (Joint Photographic Experts Group JFIF format)"
    }

    boolean detect() {
        boolean isJPEG = super.detect()
        if (isJPEG) { //check if not MRXS (fake JPEG)
            String imageAbsolutePath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)
            File slideFile = new File(imageAbsolutePath)
            if (slideFile.canRead()) {
                try {
                    return !OpenSlide.detectVendor(slideFile)
                } catch (java.io.IOException e) {
                    //Not a file that OpenSlide can recognize
                    return true
                }
            } else {
                //throw ERROR reading file
            }
        }


    }
}
