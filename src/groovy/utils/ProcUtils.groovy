package utils

import groovy.util.logging.Log
import groovy.util.logging.Log4j

/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
 *
 * Licensed under the GNU Lesser General Public License, Version 2.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * User: lrollus
 * Date: 19/09/13
 * GIGA-ULg
 *
 */
@Log4j
class ProcUtils {

    static def executeOnShell(String command) {
        return executeOnShell(command, new File("/"))
    }

    static def executeOnShell(String command, File workingDir) {
        log.info("Will execute $command")

        def process = new ProcessBuilder(addShellPrefix(command))
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()

        process.inputStream.eachLine { log.debug("-- $it") }
        process.waitFor()

        int value = process.exitValue()
        log.info("Command exited with $value")
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
