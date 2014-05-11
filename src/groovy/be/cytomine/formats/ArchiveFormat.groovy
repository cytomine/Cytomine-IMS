package be.cytomine.formats

/**
 * Created by stevben on 10/05/14.
 */
abstract class ArchiveFormat extends Format {

    abstract public String[] extract(String destPath)
}
