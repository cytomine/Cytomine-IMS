package be.cytomine.formats.standard
/**
 * Created by stevben on 22/04/14.
 */
class JPEG2000Format extends CommonFormat {

    public JPEG2000Format() {
        extensions = ["jp2"]
        mimeType = "image/jp2"
    }

    public boolean detect() {
        ""
    }


}
