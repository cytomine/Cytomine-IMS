package utils

/*
 * Copyright (c) 2009-2019. Authors: see NOTICE file.
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

import groovy.util.logging.Log4j

@Log4j
class ProcUtils {
    //https://stackoverflow.com/a/25337451
    static def executeOnShell(def command) {
        log.info("Will execute $command")

        def proc = command.execute()
        def outputStream = new StringBuilder()
        def errorStream = new StringBuilder()
        proc.waitForProcessOutput(outputStream, errorStream)

        log.info("Command exited with ${proc.exitValue()}")
        log.debug(outputStream)
        if (proc.exitValue() != 0)
            log.warn(errorStream)

        return [
                exit: proc.exitValue(),
                out : outputStream.toString(),
                err : errorStream.toString(),
                all : outputStream.toString() + errorStream.toString()
        ]
    }
}
