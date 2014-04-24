package be.cytomine.formats.digitalpathology

import be.cytomine.client.models.UploadedFile
import be.cytomine.formats.CytomineFormat

/**
 * Created by stevben on 22/04/14.
 */
abstract class OpenSlideCompatibleSingleFileFormat extends CytomineFormat {

    UploadedFile[] handle() {
        return [uploadedFile] //nothing to do, the format is understood by IIP+OpenSlide natively
    }
}
