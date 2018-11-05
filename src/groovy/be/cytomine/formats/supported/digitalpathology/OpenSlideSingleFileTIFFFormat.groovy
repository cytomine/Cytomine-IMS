package be.cytomine.formats.supported.digitalpathology

class OpenSlideSingleFileTIFFFormat extends OpenSlideSingleFileFormat {
    def fakeExtension = ""

    File rename() {
        String filename
        if(absoluteFilePath.lastIndexOf('.') > -1)
            filename = absoluteFilePath.substring(0, absoluteFilePath.lastIndexOf('.')) + "." + fakeExtension
        else
            filename = absoluteFilePath + "." + fakeExtension

        def renamed = new File(filename)
        if (!renamed.exists())
            "ln -s $absoluteFilePath $renamed".execute()
        return renamed
    }
}
