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

    protected def cytominePropertyKeys = [
            "cytomine.width": null,
            "cytomine.height": null,
            "cytomine.depth": null,
            "cytomine.duration": null,
            "cytomine.channels": null,
            "cytomine.physicalSizeX": null,
            "cytomine.physicalSizeY": null,
            "cytomine.physicalSizeZ": null,
            "cytomine.magnification": null,
            "cytomine.framePerSecond": null,
            "cytomine.bitPerSample": null,
            "cytomine.samplePerPixel": null,
            "cytomine.colorspace": null
    ]

    def parseString = { x -> x }
    def parseInt = { x -> Integer.parseInt(x) }
    def parseDouble = { x -> Double.parseDouble(x.replaceAll(",", ".")) }
    protected def cytominePropertyParsers = [
            "cytomine.width": parseInt,
            "cytomine.height": parseInt,
            "cytomine.depth": parseInt,
            "cytomine.duration": parseInt,
            "cytomine.channels": parseInt,
            "cytomine.physicalSizeX": parseDouble,
            "cytomine.physicalSizeY": parseDouble,
            "cytomine.physicalSizeZ": parseDouble,
            "cytomine.magnification": parseInt,
            "cytomine.framePerSecond": parseDouble,
            "cytomine.bitPerSample": parseInt,
            "cytomine.samplePerPixel": parseInt,
            "cytomine.colorspace": parseString
    ]

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
        def properties = [:]
        properties << ["cytomine.mimeType": this.mimeType]
        properties << ["cytomine.extension": this.file.extension()]
        properties << ["cytomine.format": this.toString()]
        return properties
    }

    def annotations() {
        return []
    }
}
