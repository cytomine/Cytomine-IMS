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
        println "Create path=$path"
        int value = ProcUtils.executeOnShell("mkdir -p " + path)
        println "Create right=$path"
        ProcUtils.executeOnShell("chmod -R 777 " + path)
        return value
    }

    def deleteFile(String path) {
        def deleteCommand = "rm " + path
        log.info deleteCommand
        def proc = deleteCommand.execute()
        proc.waitFor()
        return proc.exitValue()
    }

    def rename(String source, String target) {
        def executable = "mv"
        def command = """$executable $source $target"""
        log.info "$command"
        return ProcUtils.executeOnShell(command) == 0
    }

}
