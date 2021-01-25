package be.cytomine.formats

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

import be.cytomine.formats.tools.CytomineFile
import be.cytomine.formats.tools.histogram.VipsHistogramExtractor
import be.cytomine.formats.tools.metadata.ExifToolMetadataExtractor
import be.cytomine.units.Length
import be.cytomine.units.MetricPrefix
import groovy.util.logging.Log4j
import utils.PropertyUtils

import java.awt.image.BufferedImage

@Log4j
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

    protected def cytominePropertyKeys = [
            (PropertyUtils.CYTO_WIDTH)        : null,
            (PropertyUtils.CYTO_HEIGHT)       : null,
            (PropertyUtils.CYTO_DEPTH)        : null,
            (PropertyUtils.CYTO_DURATION)     : null,
            (PropertyUtils.CYTO_CHANNELS)     : null,
            (PropertyUtils.CYTO_X_RES)        : null,
            (PropertyUtils.CYTO_Y_RES)        : null,
            (PropertyUtils.CYTO_Z_RES)        : null,
            (PropertyUtils.CYTO_X_RES_UNIT)   : null,
            (PropertyUtils.CYTO_Y_RES_UNIT)   : null,
            (PropertyUtils.CYTO_Z_RES_UNIT)   : null,
            (PropertyUtils.CYTO_MAGNIFICATION): null,
            (PropertyUtils.CYTO_FPS)          : null,
            (PropertyUtils.CYTO_BPS)          : null,
            (PropertyUtils.CYTO_SPP)          : null,
            (PropertyUtils.CYTO_COLORSPACE)   : ""
    ]

    protected def cytominePropertyParsers = [
            (PropertyUtils.CYTO_WIDTH)        : PropertyUtils.parseInt,
            (PropertyUtils.CYTO_HEIGHT)       : PropertyUtils.parseInt,
            (PropertyUtils.CYTO_DEPTH)        : PropertyUtils.parseInt,
            (PropertyUtils.CYTO_DURATION)     : PropertyUtils.parseInt,
            (PropertyUtils.CYTO_CHANNELS)     : PropertyUtils.parseInt,
            (PropertyUtils.CYTO_X_RES)        : PropertyUtils.parseDouble,
            (PropertyUtils.CYTO_Y_RES)        : PropertyUtils.parseDouble,
            (PropertyUtils.CYTO_Z_RES)        : PropertyUtils.parseDouble,
            (PropertyUtils.CYTO_X_RES_UNIT)   : PropertyUtils.parseString,
            (PropertyUtils.CYTO_Y_RES_UNIT)   : PropertyUtils.parseString,
            (PropertyUtils.CYTO_Z_RES_UNIT)   : PropertyUtils.parseString,
            (PropertyUtils.CYTO_MAGNIFICATION): PropertyUtils.parseInt,
            (PropertyUtils.CYTO_FPS)          : PropertyUtils.parseDouble,
            (PropertyUtils.CYTO_BPS)          : PropertyUtils.parseInt,
            (PropertyUtils.CYTO_SPP)          : PropertyUtils.parseInt,
            (PropertyUtils.CYTO_COLORSPACE)   : PropertyUtils.parseString
    ]

    String toString() {
        return this.class.simpleName
    }

    abstract boolean detect()

    CytomineFile getFile() {
        return file
    }

    void setFile(CytomineFile file) {
        this.file = file
    }

    def properties() {
        return new ExifToolMetadataExtractor(this.file).properties()
    }

    def cytomineProperties() {
        def properties = properties()
        properties << [(PropertyUtils.CYTO_MIMETYPE): this.mimeType]
        properties << [(PropertyUtils.CYTO_EXT): this.file.extension()]
        properties << [(PropertyUtils.CYTO_FORMAT): this.toString()]

        cytominePropertyKeys.each { cytoKey, tagKey ->
            if (!tagKey || properties.hasProperty(tagKey as String))
                return
            def value = properties.get(tagKey)
            if (value) {
                properties << [(cytoKey): cytominePropertyParsers.get(cytoKey)(value)]
            }
        }

        properties = properties.findAll { it.value != null && !(it as String).isEmpty() }

        // Convert resolutions to micron per pixel
        def resolutions = [
                [valueKey: PropertyUtils.CYTO_X_RES, unitKey: PropertyUtils.CYTO_X_RES_UNIT],
                [valueKey: PropertyUtils.CYTO_Y_RES, unitKey: PropertyUtils.CYTO_Y_RES_UNIT],
                [valueKey: PropertyUtils.CYTO_Z_RES, unitKey: PropertyUtils.CYTO_Z_RES_UNIT],
        ]
        resolutions.each { resolution ->
            def value = (Double) properties[resolution.valueKey]
            def unit = (String) properties[resolution.unitKey]
            try {
                properties[resolution.valueKey] = new Length(value, unit).to(MetricPrefix.MICRO)
                properties[resolution.unitKey] = (properties[resolution.valueKey]) ? MetricPrefix.MICRO.symbol + "m" : null
            }
            catch (IllegalArgumentException ignored) {
                // Values in properties are inconsistent.
                properties.remove(resolution.valueKey)
                properties.remove(resolution.unitKey)
            }
        }

        return properties.findAll { it.value != null && !(it as String).isEmpty() }
    }

    def histogram() {
        histogram(0)
    }

    def histogram(int band) {
        return new VipsHistogramExtractor(this.file).histogram(band)
    }

    def annotations() {
        return []
    }

    /**
     * Get the list of available associated labels.
     * @return List<String>  the labels
     */
    def associated() {
        return []
    }

    /**
     * Get an image associated to a label.
     * @param label The label
     * @return A buffered image for this associated file
     */
    BufferedImage associated(def label) {
        if (!label in associated())
            return null

        return null
    }
}
