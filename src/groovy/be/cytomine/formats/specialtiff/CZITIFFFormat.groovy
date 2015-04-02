package be.cytomine.formats.specialtiff

/**
 * Created by hoyoux on 31.03.15.
 *
 * TIFF images converted from CZI images by the Harvard converter http://hcbi.fas.harvard.edu/resources_software
 */
class CZITIFFFormat extends TIFFToConvert {

    public CZITIFFFormat () {
        extensions = ["tif", "tiff"]
    }

    public boolean detect() {
        String tiffinfo = getTiffInfo()
        return tiffinfo.contains("ImageDescription: Label") && tiffinfo.contains("ImageDescription: SlidePreview")
    }
}
