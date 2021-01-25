package be.cytomine.units

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

class Length {
    private static final double INCH_TO_METER = 0.0254

    MetricPrefix prefix
    Double value
    boolean isInches

    /**
     * A length with a unit given with in natural English.
     * @param value
     * @param unit  A unit in natural English ("Millimeters", "nm", "centimeter", "inches", ...)
     */
    public Length(Double value, String unit) {
        this.value = value
        this.isInches = ['inch', 'inches', 'in', '"'].contains(unit?.trim()?.toLowerCase())
        this.prefix = (this.isInches) ? MetricPrefix.DEFAULT : MetricPrefix.findPrefix(prefixFromUnit(unit))

        if (!this.value) {
            throw new IllegalArgumentException("No value for length.")
        }

        if (!this.prefix) {
            throw new IllegalArgumentException("No metric prefix found for length.")
        }
    }

    public double toMeters() {
        def v = (isInches) ? this.value * INCH_TO_METER : this.value
        return v * Math.pow(10, prefix?.exponent)
    }

    public double to(MetricPrefix toPrefix) {
        return this.toMeters() * Math.pow(10, -toPrefix.exponent)
    }

    static String prefixFromUnit(String unit) {
        unit = unit?.trim()?.toLowerCase() ?: ""
        if (unit.length() > 1 && unit.length() <= 3 && unit[-1] == 'm') {
            return unit[0..-2]
        }

        return unit - "meters" - "meter"
    }
}
