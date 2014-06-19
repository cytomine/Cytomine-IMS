package be.cytomine.formats

/**
 * Created by stevben on 22/04/14.
 */
abstract class Format {

    public String[] extensions = null
    public String mimeType = nulls
    public String absoluteFilePath

    abstract public boolean detect()


}
