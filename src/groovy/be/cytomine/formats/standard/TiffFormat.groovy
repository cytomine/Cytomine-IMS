package be.cytomine.formats.standard

import be.cytomine.client.models.UploadedFile
import be.cytomine.formats.CytomineFormat
import org.springframework.util.StringUtils

/**
 * Created by stevben on 22/04/14.
 */
class TIFFFormat extends CytomineFormat {


    private enum TIFF_KIND {
        UNDEFINED,
        SINGLE_PLANE,
        MULTI_PLANE,
        VENTANA
    }

    private int type = TIFF_KIND.UNDEFINED

    public TIFFFormat() {
        extensions = ["tif", "tiff"]
    }

    public boolean detect() {
        String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)
        String tiffinfo = "tiffinfo $originalFilenameFullPath".execute().text
        //we have a TIFF, but what kind ? flat, pyramid, multi-page, ventana ?

        if (tiffinfo.contains("<iScan")) { //ventana
            type = TIFF_KIND.VENTANA
        }
        int nbTiffDirectory = StringUtils.countOccurrencesOf(tiffinfo, "TIFF Directory")

        if (nbTiffDirectory == 1) { //single layer tiff, we ne need to create a pyramid version
            type = TIFF_KIND.SINGLE_PLANE
        } else if (nbTiffDirectory > 1) { //pyramid or multi-page
            //how to determine the kind of tiff?
            type = TIFF_KIND.MULTI_PLANE
        }

        return (tiffinfo.contains("Not a TIFF"))

    }

    public UploadedFile[] handle() {
        return null //nothing to do if alreayd pyramid else vipsify ?
    }
}
