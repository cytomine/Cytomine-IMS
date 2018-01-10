package be.cytomine.processing.merge;

import be.cytomine.exception.MiddlewareException;
import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;
import ij.process.LUT;

import java.awt.*;

// Based on https://github.com/imagej/imagej1/blob/master/ij/plugin/RGBStackMerge.java#L262
public class CytomineStackMerge /*extends RGBStackMerge*/ {

    private ImagePlus imp;
    private boolean ignoreLuts;

    public static ImagePlus merge(ImagePlus[] images, Color[] imageColors) throws Exception {
        CytomineStackMerge rgbsm = new CytomineStackMerge();
        return rgbsm.mergeChannelsWithLUT(images, imageColors);
    }

    /**
     * Merge flat (no z or t dimension) gray images after colorize them (Function Colors & Merge Channel in ImageJ)
     * @param images
     * @param imageColors
     * @return
     * @throws Exception
     */
    public ImagePlus mergeChannelsWithLUT(ImagePlus[] images, Color[] imageColors) throws Exception {

        if(images.length!=imageColors.length)
            throw new Exception("Image number ("+images.length+") != Color number ("+imageColors.length+"): 1 image = 1 color!");

        int channels = images.length;
        if (channels<2) throw new MiddlewareException("Cannot merge less than 2 images");

        for (int i=0; i<channels; i++) {
            if (images[i]==null) throw new MiddlewareException("Cannot merge images if one is null");
        }

        Color[] colors = imageColors;
        ImagePlus imp = images[0];
        int w = imp.getWidth();
        int h = imp.getHeight();
        int slices = 1;//imp.getNSlices();
        int frames = 1;//imp.getNFrames();
        ImageStack stack = new ImageStack(w, h);

        for (int i=0; i<channels; i++) {
            ImagePlus imp2 = images[i];
            if (isDuplicate(i,images))
                imp2 = imp2.duplicate();

            ImageProcessor ip = imp2.getStack().getProcessor(1);
            stack.addSlice(null, ip);
        }

        ImagePlus imp2 = new ImagePlus(null, stack);
        imp2.setDimensions(channels, slices, frames);

        imp2 = new CompositeImage(imp2, CompositeImage.COMPOSITE);

        for (int c=0; c<channels; c++) {
            ImageProcessor ip = images[c].getProcessor();

            LUT lut = LUT.createLutFromColor(colors[c]);
            lut.min = ip.getMin();
            lut.max = ip.getMax();
            ((CompositeImage)imp2).setChannelLut(lut, c+1);
        }
        return imp2;
    }

    private boolean isDuplicate(int index, ImagePlus[] images) {
        for (int i=0; i<index; i++) {
            if (images[index]==images[i])
                return true;
        }
        return false;
    }
}