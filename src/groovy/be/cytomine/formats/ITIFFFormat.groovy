package be.cytomine.formats

/***
* For format identified by the tiffinfo command
**/
interface ITIFFFormat {
    boolean detect(String tiffinfo)
}
