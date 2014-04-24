package be.cytomine.formats.digitalpathology

import be.cytomine.client.models.UploadedFile


/**
 * Created by stevben on 22/04/14.
 */
abstract class OpenSlideCompatibleMultipleFileFormat extends OpenSlideFormat {

    UploadedFile[] handle() {
        //1. unzip
        //2. return new uploadedFiles
    }

}
