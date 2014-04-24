package be.cytomine.formats

import be.cytomine.client.models.UploadedFile

/**
 * Created by stevben on 22/04/14.
 */
abstract class CytomineFormat {

    protected String[] extensions = null

    public UploadedFile uploadedFile

    abstract public boolean detect()
    abstract public UploadedFile[] handle()
}
