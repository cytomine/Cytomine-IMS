package be.cytomine.formats

import be.cytomine.client.models.UploadedFile

/**
 * Created by stevben on 22/04/14.
 */
abstract public class CytomineFormatIdentifier {

    private UploadedFile uploadedFile

    public CytomineFormatIdentifier(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile
    }

    abstract boolean properties()
    abstract boolean convert()
}