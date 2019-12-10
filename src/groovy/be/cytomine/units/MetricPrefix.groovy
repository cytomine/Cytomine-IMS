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

class MetricPrefix {
    public static final MetricPrefix KILO = new MetricPrefix("k", "kilo", 3)
    public static final MetricPrefix HECTO = new MetricPrefix("h", "hecto", 2)
    public static final MetricPrefix DEFAULT = new MetricPrefix("", "", 0)
    public static final MetricPrefix DECI = new MetricPrefix("d", "deci", -1)
    public static final MetricPrefix CENTI = new MetricPrefix("c", "centi", -2)
    public static final MetricPrefix MILLI = new MetricPrefix("m", "milli", -3)
    public static final MetricPrefix MICRO = new MetricPrefix("µ", "micro", -6)
    public static final MetricPrefix MICRO2 = new MetricPrefix("u", "micro", -6)
    public static final MetricPrefix NANO = new MetricPrefix("n", "nano", -9)
    public static final MetricPrefix PICO = new MetricPrefix("p", "pico", -12)

    public static def prefixes = [KILO, HECTO, DEFAULT, DECI, CENTI, MILLI, MICRO, MICRO2, NANO, PICO]

    /**
     * Get a MetricPrefix from its symbol or name.
     *
     * @param text
     * @return
     */
    public static findPrefix(String text) {
        text = text.toLowerCase()
        return prefixes.find { metric ->
            metric.symbol == text || metric.name == text
        }
    }

    String symbol
    String name
    int exponent

    private MetricPrefix(String symbol, String name, int exponent) {
        this.symbol = symbol
        this.name = name
        this.exponent = exponent
    }
}
