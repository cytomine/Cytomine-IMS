package utils

import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage

class ImageUtils {

    public static BufferedImage resize(BufferedImage img, int newW, int newH) {
        int type = img.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : img.getType()

        BufferedImage scaledImage = new BufferedImage(newW, newH, type)

        Graphics2D graphics2D = scaledImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(img , 0, 0, newW, newH, null);

        graphics2D.dispose();
        return scaledImage;
    }

    public static BufferedImage rotate90ToRight(BufferedImage inputImage) {
        int width = inputImage.getWidth()
        int height = inputImage.getHeight()
        BufferedImage returnImage = new BufferedImage(height, width, inputImage.getType())

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                returnImage.setRGB(height - y - 1, x, inputImage.getRGB(x, y))
            }
        }
        return returnImage
    }
}
