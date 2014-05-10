package be.cytomine.formats

/**
 * Created by stevben on 22/04/14.
 */
abstract class Format {

    protected String[] extensions = null

    public String uploadedFilePath

    abstract public boolean detect()

}
