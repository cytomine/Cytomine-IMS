package be.cytomine.multidim.hdf5.output
/**
 * Created by laurent on 16.12.16.
 */
public class HDF5Pixel implements  HDF5Geometry {
    private int x,y,dim;
    //Base constructor


    public HDF5Pixel(int x, int y, int dim){
        this.x = x;
        this.y = y;
        this.dim = dim;
    }

    def benchmark = { closure ->
        def start = System.currentTimeMillis()
        closure.call()
        def now = System.currentTimeMillis()
        now - start
    }

    // tilecache is an array of size 1
    def getDataFromCache(def cubeCache){
        setData( cubeCache[0].getPixelInCache(x,y))
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    def getDim(){
        return [x,y,dim]
    }
}
