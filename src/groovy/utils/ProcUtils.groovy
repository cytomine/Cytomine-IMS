package utils

/**
 * User: lrollus
 * Date: 19/09/13
 * GIGA-ULg
 *
 */
class ProcUtils {

    static def executeOnShell(String command) {
        return executeOnShell(command, new File("/"))
    }

    static def executeOnShell(String command, File workingDir) {
        println command
        def process = new ProcessBuilder(addShellPrefix(command))
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()
        process.inputStream.eachLine { println it }
        process.waitFor();
        int value = process.exitValue()
        println "Command return value = $value"
        return value
    }

    static def addShellPrefix(String command) {
        String[] commandArray = new String[3]
        commandArray[0] = "sh"
        commandArray[1] = "-c"
        commandArray[2] = command
        return commandArray
    }
}
