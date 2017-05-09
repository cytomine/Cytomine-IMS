package be.cytomine.formats.supported

import be.cytomine.formats.Format

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

import grails.util.Holders
import utils.ServerUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 10/05/14.
 */
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

    String  cropURL(def params, def charset = "UTF-8") {
        String fif = URLEncoder.encode(params.fif,charset)
        int topLeftX = params.int('topLeftX')
        int topLeftY = params.int('topLeftY')
        double width = params.double('width')
        double height = params.double('height')
        double imageWidth = params.double('imageWidth')
        double imageHeight = params.double('imageHeight')

        //All values x,y,w & h should be in ratios 0-1.0 [RGN=x,y,w,h]
        def x = (topLeftX == 0) ? 0 : 1/(imageWidth / topLeftX)
        def y = ((imageHeight - topLeftY) == 0) ? 0 : 1/(imageHeight / (imageHeight - topLeftY))
        double w = (width == 0) ? 0d : 1d/(imageWidth / width)
        double h = (height == 0) ? 0d : 1d/(imageHeight / height)

        // TODO perf: replace the previous assignment by the following
        /*def x = topLeftX/imageWidth
        def y = (imageHeight - topLeftY)/imageHeight
        double w = width/imageWidth
        double h = height/imageHeight*/

        if(x>1 || y > 1) return

		int maxWidthOrHeight = Holders.config.cytomine.maxCropSize
        if (params.maxSize) {
            int maxSize = params.int('maxSize', 256)
            if(maxWidthOrHeight > maxSize) {
                maxWidthOrHeight=maxSize;
            }
        }

        if(ServerUtils.getServers(Holders.config.cytomine.iipImageServerBase).containsAll(iipURL)){
            // with new version of iipsrv, the meaning of WID & HEI change !
            if (width > maxWidthOrHeight || height > maxWidthOrHeight) {
                return "${ServerUtils.getServer(iipURL)}?FIF=$fif&RGN=$x,$y,$w,$h&HEI=$maxWidthOrHeight&WID=$maxWidthOrHeight&CVT=jpeg"
            } else if(params.maxSize) {
                // TODO here maxSize is the "wanted size". Create a param wantedSize when all iip wiil be unified
                int maxSize = params.int('maxSize', 256)
                return "${ServerUtils.getServer(iipURL)}?FIF=$fif&RGN=$x,$y,$w,$h&HEI=$maxSize&WID=$maxSize&CVT=jpeg"

            }
            return "${ServerUtils.getServer(iipURL)}?FIF=$fif&RGN=$x,$y,$w,$h&HEI=$height&WID=$width&CVT=jpeg"

        } else {
            if (width > maxWidthOrHeight || height > maxWidthOrHeight) {
                int tmpWidth = width
                int tmpHeight = height
                while (tmpWidth > maxWidthOrHeight || tmpHeight > maxWidthOrHeight) {
                    tmpWidth = tmpWidth / 2
                    tmpHeight = tmpHeight / 2
                }
                /*
                Ruven P. (author of IIP Image)
                In fact, the region is calculated from the WID or HEI given, not from
                the full image size. So you get the requested region on the virtual
                750px resize. I guess you were expecting to get a region exactly of size
                WID?

                This is something that seems to have caused confusion with others also
                and perhaps the way it works in counter intuitive, so I'm considering
                changing the behaviour in the 1.0 release and have WID or HEI define the
                final region size rather than the virtual image size. In the meantime,
                the way to get around it is to calculate the appropriate WID that the
                full image would be. So if your image is x pixels wide, give WID the
                value of x/2 to get a 750px wide image.
                */
                int hei = imageHeight / (height / tmpHeight)
                return "${ServerUtils.getServer(iipURL)}?FIF=$fif&RGN=$x,$y,$w,$h&HEI=$hei&CVT=jpeg"
            }
            return "${ServerUtils.getServer(iipURL)}?FIF=$fif&RGN=$x,$y,$w,$h&CVT=jpeg"
        }
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
