package be.cytomine.formats.standard

import be.cytomine.client.models.UploadedFile
import be.cytomine.formats.CytomineFormat

/**
 * Created by stevben on 22/04/14.
 */
abstract class CommonFormat extends CytomineFormat {

    public IMAGE_MAGICK_FORMAT_IDENTIFIER = null

    public boolean detect() {
        String imageAbsolutePath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)
        String identify = "identify -verbose $imageAbsolutePath".execute().text
        return identify.contains(IMAGE_MAGICK_FORMAT_IDENTIFIER)
    }

    public UploadedFile[] handle() {
        return null //vipsfied image
    }
}
