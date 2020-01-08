package be.cytomine.image

/*
 * Copyright (c) 2009-2020. Authors: see NOTICE file.
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

class HistogramService {

    def min(def histogram) {
        return histogram.findIndexOf { it != 0 }
    }

    def max(def histogram) {
        return histogram.size() - 1
    }

    def binnedHistogram(def histogram, int nBins, int bps) {
        def hist = histogram
        def maxHistSize = Math.pow(2, bps)
        if (maxHistSize > hist.size()) {
            hist += [0] * (maxHistSize - hist.size())
        }

        if (maxHistSize == nBins) {
            return hist
        }

        def binSize = (int) Math.ceil(maxHistSize / (double) nBins)
        def bins = []
        for (int i = 0; i < nBins; i++) {
            def start = i * binSize
            def end = start + binSize
            bins << hist.subList(start, end).sum()
        }

        return bins
    }
}
