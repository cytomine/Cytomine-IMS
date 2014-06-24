package be.cytomine.utils

import utils.FilesUtils
import utils.ProcUtils

class FileSystemService {

    def getAbsolutePathsAndExtensionsFromPath(String path) {
        def pathsAndExtensions = []
        new File(path).eachFileRecurse() { file ->
            if (!file.directory) {
                String absolutePath = file.getAbsolutePath()
                String extension = FilesUtils.getExtensionFromFilename(file.getAbsolutePath())
                pathsAndExtensions << [absolutePath : absolutePath, extension : extension]
            }

        }
        return pathsAndExtensions
    }

    def makeLocalDirectory(String path) {
        int value = ProcUtils.executeOnShell("mkdir -p " + path)
        ProcUtils.executeOnShell("chmod -R 777 " + path)
        return value
    }

    def rename(String source, String target) {
        def executable = "mv"
        def command = """$executable $source $target"""
        log.info "$command"
        return ProcUtils.executeOnShell(command) == 0
    }

}
