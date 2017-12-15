package be.cytomine.multidim

/*
 * Copyright (c) 2009-2017. Authors: see NOTICE file.
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

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.image.Raster

class HyperSpectralImage {
    private def files = []
    private def images = []
    def width, height, depth

    HyperSpectralImage(def filenames) {
        filenames.each{ files << new File(it) }

        loadChannel(0)
        width = (images[0] as Raster).width
        height = (images[0] as Raster).height
        depth = filenames.size()
    }

    def channelsSize() {
        def sizes = files.collect { it.length() }
        return sizes.sum() as long
    }

    def meanChannelsSize() {
        def sizes = files.collect { it.length() }
        return (sizes.sum() as long) / (sizes.size() as long) as long
    }

    def loadChannel(channel) {
        loadChannels([channel])
    }

    def loadChannels(channels) {
        images.clear()
        channels.each {
            println files[it]
            BufferedImage i = ImageIO.read(files[it])
            images << (Raster) i.getData()
            i.flush()
        }
    }

    def extract(int x, int y, int width, int height) {
        int[] pixels = new int[width * height * images.size()]

        images.eachWithIndex{ im, i ->
            int[] channel = im.getSamples(x, y, width, height, 0, new int[width*height])
            System.arraycopy(channel, 0, pixels, (width * height * i), width * height)
        }

        pixels
    }
}
