package be.cytomine.formats.supported

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

import be.cytomine.formats.Format

import grails.util.Holders
import utils.ServerUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

abstract class SupportedImageFormat extends Format {

    def grailsApplication
    public String[] extensions = null
    public String mimeType = null
    public String widthProperty = "width"
    public String heightProperty = "height"
    public String resolutionProperty = "resolution"
    public String magnificiationProperty = "magnificiation"
    public List<String> iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerCyto)

    abstract BufferedImage associated(String label)
    abstract BufferedImage thumb(int maxSize)

    public String[] associated() {
        return ["macro"]
    }

    public def properties() {
        BufferedImage bufferedImage = ImageIO.read(new File(absoluteFilePath))
        def properties = [[key : "mimeType", value : mimeType]]
        properties << [ key : "cytomine.width", value : bufferedImage.getWidth() ]
        properties << [ key : "cytomine.height", value : bufferedImage.getHeight() ]
        properties << [ key : "cytomine.resolution", value : null ]
        properties << [ key : "cytomine.magnification", value : null ]
        return properties
    }

    String cropURL(def params, def charset = "UTF-8") {
        String fif = URLEncoder.encode(params.fif,charset)
        int topLeftX = params.int('topLeftX')
        int topLeftY = params.int('topLeftY')
        double width = params.double('width')
        double height = params.double('height')
        double imageWidth = params.double('imageWidth')
        double imageHeight = params.double('imageHeight')

        def x = topLeftX/imageWidth
        def y = (imageHeight - topLeftY)/imageHeight
        double w = width/imageWidth
        double h = height/imageHeight

        if(x>1 || y > 1)
            return null

		int maxWidthOrHeight = Holders.config.cytomine.maxCropSize
        if (params.maxSize) {
            int maxSize = params.int('maxSize', 256)
            if(maxWidthOrHeight > maxSize) {
                maxWidthOrHeight=maxSize;
            }
        }

        if (width > maxWidthOrHeight || height > maxWidthOrHeight) {
            return "${ServerUtils.getServer(iipURL)}?FIF=$fif&RGN=$x,$y,$w,$h&HEI=$maxWidthOrHeight&WID=$maxWidthOrHeight&CVT=jpeg"
        } else if(params.maxSize) {
            // TODO here maxSize is the "wanted size". Create a param wantedSize when all iip wiil be unified
            int maxSize = params.int('maxSize', 256)
            return "${ServerUtils.getServer(iipURL)}?FIF=$fif&RGN=$x,$y,$w,$h&HEI=$maxSize&WID=$maxSize&CVT=jpeg"

        }
        return "${ServerUtils.getServer(iipURL)}?FIF=$fif&RGN=$x,$y,$w,$h&HEI=$height&WID=$width&CVT=jpeg"
    }

    public String tileURL(fif, params) {
        return "${ServerUtils.getServer(iipURL)}?zoomify="+URLEncoder.encode(fif, "UTF-8")+"/TileGroup$params.tileGroup/$params.z-$params.x-$params.y" + ".jpg"
    }

    // TODO do it with OpenSlide or IIP ?
    protected BufferedImage rotate90ToRight( BufferedImage inputImage ){
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage returnImage = new BufferedImage( height, width , inputImage.getType()  );

        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                returnImage.setRGB( height - y - 1, x, inputImage.getRGB( x, y  )  );
            }
        }
        return returnImage;
    }
}
