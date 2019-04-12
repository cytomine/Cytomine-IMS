package be.cytomine.formats

import utils.FilesUtils

class CytomineFile extends File {

    String tiffInfoOutput
    String imageMagickOutput
    String openSlideVendor

    def c
    def z
    def t

    CytomineFile(String pathname) {
        this(pathname, null, null, null)
    }

    CytomineFile(String pathname, def c, def z, def t) {
        super(pathname)
        setDimensions(c, z, t)
    }

    CytomineFile(String parent, String child) {
        this(parent, child, null, null, null)
    }

    CytomineFile(String parent, String child, def c, def z, def t) {
        super(parent, child)
        setDimensions(c, z, t)
    }

    CytomineFile(File parent, String child) {
        this(parent, child, null, null, null)
    }

    CytomineFile(File parent, String child, def c, def z, def t) {
        super(parent, child)
        setDimensions(c, z, t)
    }

    def setDimensions(def c, def z, def t) {
        this.c = c
        this.z = z
        this.t = t
    }

    def getTiffInfoOutput() {
        if (!tiffInfoOutput)
            tiffInfoOutput = FormatUtils.getTiffInfo(this.absolutePath)
        return tiffInfoOutput
    }

    def getImageMagickOutput() {
        if (!imageMagickOutput)
            imageMagickOutput = FormatUtils.getImageMagick(this.absolutePath)
        return imageMagickOutput
    }

    def getOpenSlideVendor() {
        if (!openSlideVendor)
            openSlideVendor = FormatUtils.getOpenSlideVendor(this)
        return openSlideVendor
    }

    def extension() {
        return FilesUtils.getExtensionFromFilename(this.absolutePath).toLowerCase()
    }
}
