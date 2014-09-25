package be.cytomine.utils

import utils.ProcUtils

class FileSystemService {

    def makeLocalDirectory(String path) {
        int value = ProcUtils.executeOnShell("mkdir -p " + path)
        ProcUtils.executeOnShell("chmod -R 777 " + path)
        return value
    }


}
