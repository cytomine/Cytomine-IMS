package be.charybde.multidim.hdf5.output;

import ch.systemsx.cisd.base.mdarray.MDShortArray;
import ch.systemsx.cisd.hdf5.IHDF5Reader

import java.util.concurrent.Callable
import java.util.concurrent.Future;


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

    @Override
    String getCSV() {
        getValues().toString()
    }

    def getDim(){
        return [x,y,dim]
    }
}
