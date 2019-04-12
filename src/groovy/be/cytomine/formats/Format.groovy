package be.cytomine.formats

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

abstract class Format {

    public String[] extensions = null

    /**
     * The file path used in processing methods.
     */
    public CytomineFile file = null

    /**
     * The format mime type
     */
    public String mimeType = null

    /**
     * The degree of complexity of the detection method.
     */
    protected int detectionComplexity = 0

    public String toString() {
        return this.class.simpleName
    }

    abstract public boolean detect()

    CytomineFile getFile() {
        return file
    }

    void setFile(CytomineFile file) {
        this.file = file
    }

    def properties() {
        return []
    }

    def annotations() {
        return []
    }
}
