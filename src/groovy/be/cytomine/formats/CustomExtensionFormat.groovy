package be.cytomine.formats

import utils.ProcUtils

trait CustomExtensionFormat {

    File rename() {
        String filename
        if(this.file.absolutePath.lastIndexOf('.') > -1)
            filename = this.file.absolutePath.substring(0, this.file.absolutePath.lastIndexOf('.')) + "." + this.customExtension
        else
            filename = this.file.absolutePath + "." + this.customExtension

        def renamed = new File(filename)
        if (!renamed.exists())
            ProcUtils.executeOnShell("ln -s ${this.file.absolutePath} ${renamed.absolutePath}")
        return renamed
    }
}