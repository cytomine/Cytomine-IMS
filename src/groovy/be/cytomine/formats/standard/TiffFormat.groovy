package be.cytomine.formats.standard

import be.cytomine.client.models.UploadedFile
import be.cytomine.formats.CytomineFormat
import org.springframework.util.StringUtils

/**
 * Created by stevben on 22/04/14.
 */
abstract class TIFFFormat extends CytomineFormat {

    public TIFFFormat() {
        extensions = ["tif", "tiff"]
    }

    public UploadedFile[] handle() {
        return null //nothing to do if alreayd pyramid else vipsify ?
    }
}
