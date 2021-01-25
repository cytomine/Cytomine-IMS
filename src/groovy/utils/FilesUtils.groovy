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

class FilesUtils {

    static String[] badChars = [" ", "(", ")", "+", "*", "/", "@", "'", '"', '$', '€', '£', '°', '`', '[', ']', '#', '?', '&']

    /**
     * Get the extension of a filename
     * @param String the filename
     * @return String the file extension
     */
    static def getExtensionFromFilename(def filename) {
        def extension = ""
        def m = (filename =~ /(\.[^\.]*)$/)
        if (m.size() > 0) extension = ((m[0][0].size() > 0) ? m[0][0].substring(1).trim().toLowerCase() : "")
        return extension
    }

    /**
     * Convert the current filename to a valide filename (without bad char like '@','+',...)
     * All bad char are replaced with '_'
     * @param String the original filename
     * @return String the filename with escaped forbidden characters
     */
    static String correctFilename(def originalFilename) {
        String newFilename = originalFilename
        for (String badChar : badChars) {
            newFilename = newFilename.replace(badChar, "_")
        }
        return newFilename
    }

}
