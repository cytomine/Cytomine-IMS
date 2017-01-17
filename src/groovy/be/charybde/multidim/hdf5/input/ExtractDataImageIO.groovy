package be.charybde.multidim.hdf5.input

import ch.systemsx.cisd.base.mdarray.MDShortArray

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.image.Raster

/**
 * Created by laurent on 17.12.16.
 */

public class ExtractDataImageIO extends ExtractData{
    private String directory;
    private int dim;
    def private filenames;

    private Raster ras;


    //This is just to debug
    def benchmark = { closure ->
        def start = System.currentTimeMillis()
        closure.call()
        def now = System.currentTimeMillis()
        now - start
    }


    public ExtractDataImageIO(String d, def filenames){
        this.filenames = filenames;
        this.dim = filenames.size();
        directory =d;
        try {
            String filename = directory + "/"  + filenames[0];
            BufferedImage bf = ImageIO.read(new File(filename));
            ras = bf.getData();
        } catch (IOException e) {
            System.out.println(filenames[0] + " Not found");
        }
    }


    public int getImageWidth(){
        return ras.getWidth();
    }

    public int getImageHeight(){
        return ras.getHeight();
    }

    public int getImageDepth(){
        return this.dim;
    }


    public void getImage(int i){
        try {
            String filename = directory + "/"  + filenames[i];
            BufferedImage bf = ImageIO.read(new File(filename));
            ras = bf.getData();
        } catch (IOException e) {
            println filenames[i] + " not found"
        }

    }


    public MDShortArray extract2DTile(int startX, int startY, int wid, int hei, int depth){
        long[] dims = [wid, hei, depth];
        MDShortArray result = new MDShortArray(dims);
        return extract2DTile(startX, startY, 0, wid, hei, result)
    }

    public MDShortArray extract2DTile(int startX, int startY, int dim, int wid, int hei, MDShortArray base){
     //   println "Sx "  + startX + " Sy " + startY + " d " + dim  +" w " + wid + " hei " +hei
        if(startX + wid >= getImageWidth()){
            wid = getImageWidth() - startX
        }
        if(startY + hei >= getImageHeight()){
            hei = getImageHeight() - startY
        }
        int[] chapeau = new int[256*256]
        int[] v=  ras.getSamples(startX,startY,wid, hei, 0, chapeau)
        int id = 0
        v.each { val->
                base.set((short)val, (int) (id / 256), id % 256, dim)
                ++id
            }
        return base;
    }

}
