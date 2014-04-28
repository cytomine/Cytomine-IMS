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
        String command = "identify -verbose $imageAbsolutePath"
        def proc = command.execute()
        proc.waitFor()
        String stdout = proc.in.text
        return stdout.contains(IMAGE_MAGICK_FORMAT_IDENTIFIER)
    }

    public UploadedFile[] handle() {
        return null //vipsfied image
    }
}
