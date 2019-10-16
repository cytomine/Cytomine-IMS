package be.cytomine.image

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
import com.vividsolutions.jts.geom.Point
import com.vividsolutions.jts.geom.Polygon

import ij.ImagePlus
import ij.process.ImageConverter
import ij.process.ImageProcessor
import ij.process.PolygonFiller

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.text.DecimalFormat

class ImageProcessingService {
    static transactional = false

    BufferedImage createCropWithDraw(BufferedImage image, Geometry geometry, def params) {
        int topLeftX = params.int('topLeftX')
        int topLeftY = params.int('topLeftY')
        int width = image.getWidth()
        int height = image.getHeight()
        double x_ratio = width / params.int('width')
        double y_ratio = height / params.int('height')
        int borderWidth = params.int('thickness', (int) Math.round(2 + ((double) Math.max(width, height)) / 1000d))

        Color color = (params.color) ? new Color(Integer.parseInt(params.color.replace("0x",""),16)) : Color.BLACK

        return drawGeometries(
                image,
                [geometry],
                color,
                borderWidth,
                topLeftX,
                topLeftY,
                x_ratio,
                y_ratio,
        )
    }

    BufferedImage drawGeometries(BufferedImage image, Collection<Geometry> geometryCollection, Color c, int borderWidth, int x, int y, double x_ratio, double y_ratio) {
        for (geometry in geometryCollection) {
            if (geometry instanceof MultiPolygon) {
                MultiPolygon multiPolygon = (MultiPolygon) geometry
                for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                    geometry = multiPolygon.getGeometryN(i)
                    image = drawGeometry(image, geometry, c, borderWidth, x, y, x_ratio, y_ratio)
                }
            }
            else {
                image = drawGeometry(image, geometry, c, borderWidth, x, y, x_ratio, y_ratio)
            }
        }

        return image
    }

    BufferedImage drawGeometry(BufferedImage image, Geometry geometry, Color c, int borderWidth, int x, int y, double x_ratio, double y_ratio) {
        if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry
            image = drawPolygon(image, polygon, c, borderWidth, x, y, x_ratio, y_ratio)
        }
        else if (geometry instanceof Point) {
            Point point = (Point) geometry
            image = drawPoint(image, point, c, borderWidth, x, y, x_ratio, y_ratio)
        }
        else if (geometry instanceof LineString) {
            LineString line = (LineString) geometry
            image = drawLineString(image, line, c, borderWidth, x, y, x_ratio, y_ratio)
        }

        return image
    }

    BufferedImage drawPolygon(BufferedImage image, Polygon polygon, Color c, int borderWidth, int x, int y, double x_ratio, double y_ratio) {
        image = drawLineString(image, polygon.getExteriorRing(), c, borderWidth, x, y, x_ratio, y_ratio)
        for (def j = 0; j < polygon.getNumInteriorRing(); j++) {
            image = drawLineString(image, polygon.getInteriorRingN(j), c, borderWidth, x, y, x_ratio, y_ratio)
        }

        return image
    }

    BufferedImage drawLineString(BufferedImage image, LineString lineString, Color c, int borderWidth, int x, int y, double x_ratio, double y_ratio) {
        Path2D.Float regionOfInterest = new Path2D.Float()
        boolean isFirst = true

        Coordinate[] coordinates = lineString.getCoordinates()
        for (Coordinate coordinate : coordinates) {
            double xLocal = Math.min((coordinate.x - x) * x_ratio, image.getWidth() - 1)
            xLocal = Math.max(0, xLocal)
            double yLocal = Math.min((y - coordinate.y) * y_ratio, image.getHeight() - 1)
            yLocal = Math.max(0, yLocal)

            if (isFirst) {
                regionOfInterest.moveTo(xLocal, yLocal)
                isFirst = false
            }
            regionOfInterest.lineTo(xLocal, yLocal)
        }

        Graphics2D g2d = (Graphics2D) image.getGraphics()
        g2d.setStroke(new BasicStroke(borderWidth))
        g2d.setColor(c)
        g2d.draw(regionOfInterest)

        return image
    }

    BufferedImage drawPoint(BufferedImage image, Point point, Color c, int borderWidth, int x, int y, double x_ratio, double y_ratio) {
        Graphics g = image.createGraphics()
        g.setColor(c)
        g.setStroke(new BasicStroke(borderWidth))

        int length = 10
        double xLocal = Math.min((point.x - x) * x_ratio, image.getWidth())
        xLocal = Math.max(0, xLocal)
        double yLocal = Math.min((y - point.y) * y_ratio, image.getHeight())
        yLocal = Math.max(0, yLocal)


        g.drawLine((int) xLocal, (int) yLocal - length, (int) xLocal, (int) yLocal + length)
        g.drawLine((int) xLocal - length, (int) yLocal, (int) xLocal + length, (int) yLocal)
        g.dispose()
        return image
    }

    /**
     * Create a mask on image from a geometry.
     *
     * @param image
     * @param geometry
     * @param params
     * @param withAlpha
     * @return
     */
    BufferedImage createMask(BufferedImage image, Geometry geometry, def params, boolean withAlpha) {
        int topLeftX = params.int('topLeftX')
        int topLeftY = params.int('topLeftY')
        int width = params.int('width')
        int height = params.int('height')
        int imageHeight = params.int('imageHeight')
        int alphaPercentage = params.int('alpha', 0)
        double x_ratio = image.getWidth() / width
        double y_ratio = image.getHeight() / height

        BufferedImage mask = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB)
        mask = colorizeGeometries(
                mask,
                [geometry],
                topLeftX,
                imageHeight - topLeftY,
                x_ratio,
                y_ratio,
                imageHeight
        )

        if (withAlpha) {
            return applyMaskToAlpha(image, mask, alphaPercentage)
        }
        else {
            return mask
        }
    }

    BufferedImage applyMaskToAlpha(BufferedImage image, BufferedImage mask, int alphaPercentage = 0) {
        def alpha = Math.round(2.55 * alphaPercentage)
        int width = image.getWidth()
        int height = image.getHeight()
        int[] imagePixels = image.getRGB(0, 0, width, height, null, 0, width)
        int[] maskPixels = mask.getRGB(0, 0, width, height, null, 0, width)
        int black_rgb = Color.BLACK.getRGB()
        for (int i = 0; i < imagePixels.length; i++) {
            int color = imagePixels[i] & 0x00FFFFFF // mask away any alpha present
            int alphaValue = (maskPixels[i] == black_rgb) ? alpha : 0xFF
            int maskColor = alphaValue << 24 // shift value into alpha bits
            imagePixels[i] = color | maskColor
        }
        BufferedImage combined = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        combined.setRGB(0, 0, width, height, imagePixels, 0, width)
        return combined
    }

    BufferedImage colorizeGeometries(BufferedImage mask, Collection<Geometry> geometryCollection, int x, int y, double x_ratio, double y_ratio, int imageHeight) {
        for (geometry in geometryCollection) {
            if (geometry instanceof GeometryCollection) {
                GeometryCollection multiPolygon = (GeometryCollection) geometry
                for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                    mask = colorizeGeometry(mask, multiPolygon.getGeometryN(i), x, y, x_ratio, y_ratio, imageHeight)
                }
            }
            else {
                mask = colorizeGeometry(mask, geometry, x, y, x_ratio, y_ratio, imageHeight)
            }
        }
        return mask
    }

    BufferedImage colorizeGeometry(BufferedImage mask, Geometry geometry, int x, int y, double x_ratio, double y_ratio, int imageHeight) {
        if (geometry instanceof Polygon) {
            Polygon polygon = (Polygon) geometry
            mask = colorizePolygon(mask, polygon, x, y, x_ratio, y_ratio, imageHeight)
        }
        return mask
    }

    BufferedImage colorizePolygon(BufferedImage mask, Polygon polygon, int x, int y, double x_ratio, double y_ratio, int imageHeight) {
        mask = colorizeLineString(mask, polygon.getExteriorRing(), Color.WHITE, x, y, x_ratio, y_ratio, imageHeight)
        for (def j = 0; j < polygon.getNumInteriorRing(); j++) {
            mask = colorizeLineString(mask, polygon.getInteriorRingN(j), Color.BLACK, x, y, x_ratio, y_ratio, imageHeight)
        }
        return mask
    }

    BufferedImage colorizeLineString(BufferedImage mask, LineString lineString, Color color,
                                     int x, int y, double x_ratio, double y_ratio, imageHeight) {
        ImagePlus imagePlus = new ImagePlus("", mask)
        ImageProcessor ip = imagePlus.getProcessor()
        ip.setColor(color)

        Collection<Coordinate> coordinates = lineString.getCoordinates()
        int[] _x = new int[coordinates.size()]
        int[] _y = new int[coordinates.size()]
        coordinates.eachWithIndex { coordinate, i ->
            int xLocal = (int) Math.min((coordinate.x - x) * x_ratio, mask.getWidth())
            xLocal = Math.max(0, xLocal)
            int yLocal = (int) Math.min((imageHeight - coordinate.y - y) * y_ratio, mask.getHeight())
            yLocal = Math.max(0, yLocal)
            _x[i] = xLocal
            _y[i] = yLocal
        }
        PolygonFiller polygonFiller = new PolygonFiller()
        polygonFiller.setPolygon(_x, _y, coordinates.size())
        polygonFiller.fill(ip, new Rectangle(mask.getWidth(), mask.getHeight()))
        ip.getBufferedImage()
    }

    BufferedImage dynBinary(String url, BufferedImage bufferedImage, String method) {
        ImagePlus ip = new ImagePlus(url, bufferedImage)
        ImageConverter ic = new ImageConverter(ip)
        ic.convertToGray8()
        def at = new Auto_Threshold()
        Object[] result = at.exec(ip, method, false, false, true, false, false, false)
        ImagePlus ipThresholded = (ImagePlus) result[1]
        return ipThresholded.getBufferedImage()
    }

    BufferedImage drawScaleBar(BufferedImage image, Double imageWidth, Double resolution, Double magnification) {
        log.info "ratioWith=$ratioWith"
        log.info "resolution=$resolution"

        double ratioWidth = (double)((double)bufferedImage.getWidth() / imageWidth)

        double scaleBarSize = 100d //((double)image.getWidth()/12.5d) //scale bar will be 1/10 of the picture
        int scaleBarX = 20
        int scaleBarY = 20
        int space = scaleBarSize / 10
        int boxSizeWidth = scaleBarSize + (space * 2)
        int boxSizeHeight = scaleBarSize * 0.5

        //draw white rectangle in the bottom-left of the screen
        Graphics2D graphBox = image.createGraphics()
        graphBox.setColor(Color.WHITE)
        graphBox.fillRect(scaleBarX, image.getHeight() - boxSizeHeight - scaleBarY, boxSizeWidth, boxSizeHeight)
        graphBox.dispose()

        //draw the scale bar
        Graphics2D graphScaleBar = image.createGraphics()
        graphScaleBar.setColor(Color.BLACK)

        int xStartBar = scaleBarX + space
        int xStopBar = scaleBarX + scaleBarSize + space
        int yStartBar = image.getHeight() - Math.floor(boxSizeHeight / 2).intValue() - scaleBarY
        int yStopBar = yStartBar

        graphScaleBar.setStroke(new BasicStroke(2))
        //draw the main line of the scale bar
        graphScaleBar.drawLine(xStartBar, yStartBar, xStopBar, yStopBar)
        //draw the two vertical line
        def yTop = yStartBar - (Math.floor(scaleBarSize / 6).intValue())
        def yBottom = yStopBar + (Math.floor(scaleBarSize / 6).intValue())
        graphScaleBar.drawLine(xStartBar, yTop, xStartBar, yBottom)
        graphScaleBar.drawLine(xStopBar, yTop, xStopBar, yBottom)
        graphScaleBar.dispose()

        //draw text
        String textUp, textBelow
        int textSize = 9//8*(scaleBarSize/100)
        int textXPosition = xStartBar + (xStopBar - xStartBar) / 2 - 25
        Graphics2D graphText = image.createGraphics()
        graphText.setFont(new Font("Monaco", Font.BOLD, textSize))
        DecimalFormat f = new DecimalFormat("##.00")
        Double realSize = resolution ? (scaleBarSize / ratioWidth) * resolution : null
        if (realSize) {
            textUp = f.format(realSize) + " µm"
            graphText.setColor(Color.BLACK)
        }
        else {
            textUp = "Size unknown"
            graphText.setColor(Color.RED)
        }
        graphText.drawString(textUp, textXPosition, yStartBar - 5)

        if (magnification) {
            textBelow = f.format(magnification) + " X"
            graphText.setColor(Color.BLACK)
        }
        else {
            textBelow = "Magnitude unknown"
            textXPosition -= 25
            graphText.setColor(Color.RED)
        }

        graphText.drawString(textBelow, textXPosition, yStartBar + (5 + textSize))
        graphText.dispose()
        return image
    }

    // Resize
    BufferedImage resizeImage(BufferedImage image, int width, int height) {
        int type = image.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : image.getType()
        BufferedImage resizedImage = new BufferedImage(width, height, type)
        Graphics2D g = resizedImage.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(image, 0, 0, width, height, null)
        g.dispose()
        return resizedImage
    }

    // Scale
    BufferedImage scaleImage(BufferedImage img, Integer width, Integer height) {
        int imgWidth = img.getWidth()
        int imgHeight = img.getHeight()

        // if ratio height/imgHeight < width/imgWidth then we apply the same ratio to width => we took the smaller ratio
        if (imgWidth * height < imgHeight * width) {
            width = imgWidth * height / imgHeight
        }
        else {
            height = imgHeight * width / imgWidth
        }
        BufferedImage newImage = new BufferedImage(width, height, img.getType())
        Graphics2D g = newImage.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g.drawImage(img, 0, 0, width, height, null)
        } finally {
            g.dispose()
        }
        return newImage
    }

}
