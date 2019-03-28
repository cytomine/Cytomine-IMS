package cytomine.web

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

import be.cytomine.processing.image.filters.Auto_Threshold
import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryCollection
import com.vividsolutions.jts.geom.LineString
import com.vividsolutions.jts.geom.MultiPolygon
import ij.ImagePlus
import ij.process.ImageConverter
import ij.process.ImageProcessor
import ij.process.PolygonFiller

import java.awt.*
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.text.DecimalFormat

class ImageProcessingService {

    static transactional = false

    public BufferedImage dynBinary(String url, BufferedImage bufferedImage, String method) {
        ImagePlus ip = new ImagePlus(url, bufferedImage)
        ImageConverter ic = new ImageConverter(ip)
        ic.convertToGray8()
        def at = new Auto_Threshold()
        Object[] result = at.exec(ip, method, false, false, true, false, false, false)
        ImagePlus ipThresholded = (ImagePlus) result[1]
        return ipThresholded.getBufferedImage()
    }


    //deprecated
    public BufferedImage rotate90ToRight( BufferedImage inputImage ){
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

    public BufferedImage applyMaskToAlpha(BufferedImage image, BufferedImage mask) {
        //TODO:: document this method
        int width = image.getWidth()
        int height = image.getHeight()
        int[] imagePixels = image.getRGB(0, 0, width, height, null, 0, width)
        int[] maskPixels = mask.getRGB(0, 0, width, height, null, 0, width)
        int black_rgb = Color.BLACK.getRGB()
        for (int i = 0; i < imagePixels.length; i++)
        {
            int color = imagePixels[i] & 0x00FFFFFF; // mask away any alpha present
            int alphaValue = (maskPixels[i] == black_rgb) ? 0x00 : 0xFF
            int maskColor = alphaValue << 24 // shift value into alpha bits
            imagePixels[i] = color | maskColor
        }
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        combined.setRGB(0, 0, width, height, imagePixels, 0, width)
        return combined
    }

    public BufferedImage colorizeWindow(def params, BufferedImage window, Collection<Geometry> geometryCollection, int x, int y, double x_ratio, double y_ratio) {
        for (geometry in geometryCollection) {
            log.info "colorizeWindow 1"
            if (geometry instanceof GeometryCollection) {
                GeometryCollection multiPolygon = (GeometryCollection) geometry;
                for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                    window = colorizeWindow(params, window, multiPolygon.getGeometryN(i), x, y, x_ratio, y_ratio)
                }
            } else {
                window = colorizeWindow(params, window, geometry, x, y, x_ratio, y_ratio)
            }
        }
        return window
    }

    public BufferedImage colorizeWindow(def params, BufferedImage window,  Geometry geometry, int x, int y, double x_ratio, double y_ratio) {
        log.info "colorizeWindow 2"
        log.info geometry.class
        if (geometry instanceof com.vividsolutions.jts.geom.Polygon) {
            com.vividsolutions.jts.geom.Polygon polygon = (com.vividsolutions.jts.geom.Polygon) geometry;
            window = colorizeWindow(params, window, polygon, x, y, x_ratio, y_ratio)
        }

        return window
    }

    public BufferedImage colorizeWindow(def params, BufferedImage window, com.vividsolutions.jts.geom.Polygon polygon, int x, int y, double x_ratio, double y_ratio) {
        log.info "colorizeWindow 3"
        window = colorizeWindow(params, window, polygon.getExteriorRing(), Color.WHITE, x, y, x_ratio, y_ratio)
        for (def j = 0; j < polygon.getNumInteriorRing(); j++) {
            window = colorizeWindow(params, window, polygon.getInteriorRingN(j), Color.BLACK, x, y, x_ratio, y_ratio)
        }

        return window
    }

    public BufferedImage colorizeWindow(def params, BufferedImage window, LineString lineString, Color color, int x, int y, double x_ratio, double y_ratio) {
        log.info "colorizeWindow FINAL"
        int imageHeight = params.int('imageHeight')
        ImagePlus imagePlus = new ImagePlus("", window)
        ImageProcessor ip = imagePlus.getProcessor()
        ip.setColor(color)
        //int[] pixels = (int[]) ip.getPixels()

        Collection<Coordinate> coordinates = lineString.getCoordinates()
        int[] _x = new int[coordinates.size()]
        int[] _y = new int[coordinates.size()]
        coordinates.eachWithIndex { coordinate, i ->
            int xLocal = Math.min((coordinate.x - x) * x_ratio, window.getWidth());
            xLocal = Math.max(0, xLocal)
            int yLocal = Math.min((imageHeight - coordinate.y - y) * y_ratio, window.getHeight());
            yLocal = Math.max(0, yLocal)
            _x[i] = xLocal
            _y[i] = yLocal
        }
        PolygonFiller polygonFiller = new PolygonFiller()
        polygonFiller.setPolygon(_x, _y, coordinates.size())
        polygonFiller.fill(ip, new Rectangle(window.getWidth(), window.getHeight()))
        //ip.setPixels(pixels)
        ip.getBufferedImage()

    }

    public BufferedImage drawPolygons(BufferedImage bufferedImage, Collection<Geometry> geometryCollection, Color c, int borderWidth, int x, int y, double x_ratio, double y_ratio) {
        for (geometry in geometryCollection) {

            if (geometry instanceof MultiPolygon) {
                MultiPolygon multiPolygon = (MultiPolygon) geometry;
                for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                    bufferedImage = drawPolygon(bufferedImage, multiPolygon.getGeometryN(i), c, borderWidth, x, y, x_ratio, y_ratio)
                }
            } else {
                bufferedImage = drawPolygon(bufferedImage, geometry, c, borderWidth, x, y, x_ratio, y_ratio)
            }
        }

        return bufferedImage
    }

    public BufferedImage scaleImage(BufferedImage img, Integer width, Integer height) {
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        // if ratio height/imgHeight < width/imgWidth then we apply the same ratio to width => we took the smaller ratio
        if (imgWidth*height < imgHeight*width) {
            width = imgWidth*height/imgHeight;
        } else {
            height = imgHeight*width/imgWidth;
        }
        BufferedImage newImage = new BufferedImage(width, height,img.getType());
        Graphics2D g = newImage.createGraphics();
//        g.setBackground (color);
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(img, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return newImage;
    }

    public BufferedImage drawPolygon(BufferedImage window, Geometry geometry, Color c, int borderWidth, int x, int y, double x_ratio, double y_ratio) {
        if(geometry instanceof com.vividsolutions.jts.geom.Polygon) {
            com.vividsolutions.jts.geom.Polygon polygon = (com.vividsolutions.jts.geom.Polygon) geometry
            window = drawPolygon(window, polygon, c, borderWidth, x, y, x_ratio, y_ratio)
        }

        return window
    }

    public BufferedImage drawPolygon(BufferedImage window, com.vividsolutions.jts.geom.Polygon polygon, Color c, int borderWidth, int x, int y, double x_ratio, double y_ratio) {
        window = drawPolygon(window, polygon.getExteriorRing(), c,borderWidth, x, y, x_ratio, y_ratio)
        for (def j = 0; j < polygon.getNumInteriorRing(); j++) {
            window = drawPolygon(window, polygon.getInteriorRingN(j), c,borderWidth, x, y, x_ratio, y_ratio)
        }

        return window
    }

    public BufferedImage drawPolygon(BufferedImage window, LineString lineString, Color c, int borderWidth, int x, int y, double x_ratio, double y_ratio) {

        Path2D.Float regionOfInterest = new Path2D.Float();
        boolean isFirst = true;

        Coordinate[] coordinates = lineString.getCoordinates();

        int width = window.getWidth()
        int height = window.getHeight()

        for(Coordinate coordinate:coordinates) {
            double xLocal = Math.min((coordinate.x - x)*x_ratio, width - 1);
            xLocal = Math.max(0, xLocal)
            double yLocal = Math.min((y - coordinate.y)*y_ratio, height - 1);
            yLocal = Math.max(0, yLocal)

            if(isFirst) {
                regionOfInterest.moveTo(xLocal,yLocal);
                isFirst = false;
            }
            regionOfInterest.lineTo(xLocal,yLocal);
        }
        Graphics2D g2d = (Graphics2D)window.getGraphics();
        //g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setStroke(new BasicStroke(borderWidth));
        g2d.setColor(c);

        g2d.draw(regionOfInterest);
        window
    }

    public BufferedImage drawScaleBar(BufferedImage image, Double resolution, Double ratioWith, Double magnification) {

        log.info "ratioWith=$ratioWith"
        log.info "resolution=$resolution"

        double scaleBarSize = 100d//((double)image.getWidth()/12.5d) //scale bar will be 1/10 of the picture
        int sclaBarXPosition = 20
        int sclaBarYPosition = 20	    
			
        log.info "scaleBarSize=$scaleBarSize"

        int space = scaleBarSize/10
        int boxSizeWidth = scaleBarSize + (space*2)
        int boxSizeHeight = scaleBarSize * 0.5

        //draw white rectangle in the bottom-left of the screen
        Graphics2D graphBox = image.createGraphics();
        graphBox.setColor(Color.WHITE);
        graphBox.fillRect(sclaBarXPosition, image.getHeight()-boxSizeHeight - sclaBarYPosition, boxSizeWidth, boxSizeHeight);
        graphBox.dispose();

        //draw the scale bar
        Graphics2D graphScaleBar = image.createGraphics();
        graphScaleBar.setColor(Color.BLACK);

        int xStartBar = sclaBarXPosition + space;
        int xStopBar = sclaBarXPosition + scaleBarSize + space;
        int yStartBar = image.getHeight() - Math.floor(boxSizeHeight/2).intValue() - sclaBarYPosition
        int yStopBar = yStartBar
		
		graphScaleBar.setStroke(new BasicStroke(2));
        //draw the main line of the scale bar
        graphScaleBar.drawLine( xStartBar,yStartBar, xStopBar,yStopBar);
        //draw the two vertical line
        graphScaleBar.drawLine(xStartBar,yStartBar-(Math.floor(scaleBarSize/6).intValue()),xStartBar,yStopBar+(Math.floor(scaleBarSize/6).intValue()));
        graphScaleBar.drawLine(xStopBar,yStartBar-(Math.floor(scaleBarSize/6).intValue()),xStopBar,yStopBar+(Math.floor(scaleBarSize/6).intValue()));

        graphScaleBar.dispose();

        Double realSize = resolution ? (scaleBarSize / ratioWith) * resolution : null

        log.info "realSize=$realSize"

        DecimalFormat f = new DecimalFormat("##.00");
        String textUp, textBelow;
        //draw text
        int textSize = 9//8*(scaleBarSize/100)
		int textXPosition =  xStartBar + (xStopBar - xStartBar)/2 - 25
        Graphics2D graphText = image.createGraphics();
        graphText.setFont(new Font( "Monaco", Font.BOLD, textSize ));

        if(realSize) {
            textUp = f.format(realSize) + " µm"
            graphText.setColor(Color.BLACK);
        } else{
            textUp = "Size unknown"
            graphText.setColor(Color.RED);
        }
        graphText.drawString(textUp, textXPosition, yStartBar-5)

        if(magnification) {
            textBelow = f.format(magnification) + " X"
            graphText.setColor(Color.BLACK);
        } else{
            textBelow = "Magnitude unknown"
            textXPosition -= 25;
            graphText.setColor(Color.RED);
        }

        graphText.drawString(textBelow, textXPosition, yStartBar+(5+textSize))
        graphText.dispose();
        return image
    }


    public BufferedImage createMask(BufferedImage bufferedImage, Geometry geometry, def params, boolean withAlpha) {
        int topLeftX = params.int('topLeftX')
        int topLeftY = params.int('topLeftY')
        int width = params.int('width')
        int height = params.int('height')
        int imageHeight = params.int('imageHeight')
        BufferedImage mask = new BufferedImage(bufferedImage.getWidth(),bufferedImage.getHeight(),BufferedImage.TYPE_INT_ARGB);
        double x_ratio = bufferedImage.getWidth() / width
        double y_ratio = bufferedImage.getHeight() / height

        mask = colorizeWindow(params, mask, [geometry], topLeftX, imageHeight - topLeftY, x_ratio, y_ratio)

        if (withAlpha) {
            return applyMaskToAlpha(bufferedImage, mask)
        } else {
            return mask
        }

    }
    public BufferedImage createCropWithDraw(BufferedImage bufferedImage, Geometry geometry, def params) {
        int topLeftX = params.int('topLeftX')
        int topLeftY = params.int('topLeftY')
        int width = bufferedImage.getWidth()
        int height = bufferedImage.getHeight()

        double x_ratio = width / params.int('width')
        double y_ratio = height / params.int('height')

        int borderWidth
        Integer thickness = params.int('thickness')
        if(!thickness) {
            borderWidth = Math.round(2 + ((double) Math.max(width, height)) / 1000d)
        } else {
            borderWidth = thickness
        }

        Color color = Color.BLACK;
        if(params.color) color = new Color(Integer.parseInt(params.color.replace("0x",""),16))

        bufferedImage = drawPolygons(
                bufferedImage,
                [geometry],
                color,
                borderWidth,
                topLeftX,
                topLeftY,
                x_ratio,
                y_ratio
        )
        bufferedImage
    }



}
