package be.cytomine.formats.digitalpathology

/**
 * Created by stevben on 22/04/14.
 */
class SakuraSVSlideFormat extends OpenSlideSingleFileFormat {

    public SakuraSVSlideFormat () {
        extensions = ["svslide"]
        vendor = "sakura"
        mimeType = "sakura/svslide"
    }

}
