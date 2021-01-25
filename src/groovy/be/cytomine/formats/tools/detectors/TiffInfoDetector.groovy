package be.cytomine.formats.tools.detectors

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
trait TiffInfoDetector extends Detector {
    def requiredKeywords
    def forbiddenKeywords
    def possibleKeywords

    boolean detect() {
        requiredKeywords = this.hasProperty("requiredKeywords")?.getProperty(this) ?: []
        forbiddenKeywords = this.hasProperty("forbiddenKeywords")?.getProperty(this) ?: []
        possibleKeywords = this.hasProperty("possibleKeywords")?.getProperty(this) ?: []

        def output = this.file.getTiffInfoOutput()

        /**
         * required = [a, b, c]
         * forbidden = [x, y, z]
         * possible = [m, n]
         *
         * detected = (a && b && c) && (!x && !y && !z) && (m || n) = (a && b && c) && !(x || y || z) && (m || n)
         */
        return requiredKeywords.every { output.contains(it as String) } &&
                !forbiddenKeywords.any { output.contains(it as String) } &&
                (possibleKeywords.isEmpty() ? true : possibleKeywords.any { output.contains(it as String) })
    }
}