package be.cytomine.formats.standard

import be.cytomine.client.models.UploadedFile
import be.cytomine.formats.CytomineFormat

/**
 * Created by stevben on 22/04/14.
 */
class JPEG2000Format extends CytomineFormat {

    public boolean detect() {
        ""
    }

    public UploadedFile[] handle() {
        return null //nothing to do
    }
}
