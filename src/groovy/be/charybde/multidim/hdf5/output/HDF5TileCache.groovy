package be.charybde.multidim.hdf5.output;

import ch.systemsx.cisd.base.mdarray.MDShortArray
import ncsa.hdf.hdf5lib.exceptions.HDF5SymbolTableException

import java.util.concurrent.Callable
import java.util.concurrent.Future;


/**
 * Created by laurent on 16.12.16.
 */
public class HDF5TileCache  {
    private int dim
    def cache
    private String name
    private int x_start, x_end, y_start, y_end
    private Boolean dataPresent

    public HDF5TileCache(int dim, def name){
        this.dataPresent = false
        this.cache = new ArrayList<MDShortArray>()
        this.dim = dim;
        this.name = name
        def xxyy = name.substring(1).split("_")
        x_start = Integer.parseInt(xxyy[0]) * 256
        x_end = x_start + 255
        y_start = Integer.parseInt(xxyy[1]) * 256
        y_end = y_start + 255
    }

    def benchmark = { closure ->
        def start = System.currentTimeMillis()
        closure.call()
        def now = System.currentTimeMillis()
        now - start
    }


    public void extractValues(HDF5FileReader reader){
        def tile_d = reader.getTileDepth()
        int nr_depth_tiles = dim / tile_d;
        def noError = true


        ArrayList<Future> spectra =  []
        (0..nr_depth_tiles - 1).each { i ->
            spectra << reader.getThreadPool().submit({ ->
                try{
                    reader.getReader(i).int16().readMDArray("/r"+i+"/"+name)
                }
                catch(HDF5SymbolTableException e){
                    noError = false
                }
            } as Callable)
        }

        spectra.each { res ->
            cache << res.get()
        }
        if(noError)
            dataPresent = true
    }

    int getXStart() {
        return x_start
    }

    int getXEnd() {
        return x_end
    }

    int getYStart() {
        return y_start
    }

    int getYEnd() {
        return y_end
    }

    def isDataPresent(){
        return dataPresent
    }
//TODO numbers
    def getPixelInCache(int x, int y){
        def res = []
        cache.each{ cc ->
            for(int i=0; i  < 256; ++i){
                res << cc.get(x%256,y%256,i)
            }
        }
        return res
    }
}
