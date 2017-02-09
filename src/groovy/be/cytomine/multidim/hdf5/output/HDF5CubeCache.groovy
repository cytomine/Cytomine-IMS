package be.cytomine.multidim.hdf5.output;

import ch.systemsx.cisd.base.mdarray.MDShortArray
import ncsa.hdf.hdf5lib.exceptions.HDF5SymbolTableException

import java.util.concurrent.Callable
import java.util.concurrent.Future;


/**
 * Created by laurent on 16.12.16.
 */
public class HDF5CubeCache {
    private int dim
    def cache
    private String name
    private int x_start, x_end, y_start, y_end
    private Boolean dataPresent
    private long last_use

    public HDF5CubeCache(int dim, def name, int x_size, int y_size){
        this.dataPresent = false
        this.cache = new ArrayList<MDShortArray>()
        this.dim = dim;
        this.name = name
        def xxyy = name.substring(1).split("_")
        x_start = Integer.parseInt(xxyy[0]) * x_size
        x_end = x_start + x_size - 1
        y_start = Integer.parseInt(xxyy[1]) * y_size
        y_end = y_start + y_size - 1
        last_use = System.currentTimeMillis()
    }

    def benchmark = { closure ->
        def start = System.currentTimeMillis()
        closure.call()
        def now = System.currentTimeMillis()
        now - start
    }


    public void extractValues(HDF5FileReader reader){
        int nr_depth_cube = reader.getNumberOfImage()
        def noError = true
        def oom = false

        ArrayList<Future> spectra =  []
        (0..nr_depth_cube - 1).each { i ->
            spectra << reader.getThreadPool().submit({ ->
                try{
                    reader.getReader(i).int16().readMDArray("/r"+i+"/"+name)
                }
                catch(HDF5SymbolTableException e){
                    noError = false
                }
                catch(OutOfMemoryError er){
                    println "OOM"
                    noError = false
                    oom = true
                }
            } as Callable)
        }

        spectra.each { res ->
            cache << res.get()
        }
        if(oom)
            reader.adaptCacheSize()

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

    def lastUse(){
        return last_use
    }

    def getPixelInCache(int x, int y){
        def res = []
        cache.each{ cc ->
            def dims =  cc.longDimensions()
            for(int i=0; i  < dims[2]; ++i){
                res << cc.get((int) x%dims[0], (int) y%dims[1],i)
            }
        }
        last_use = System.currentTimeMillis()
        return res
    }
}
