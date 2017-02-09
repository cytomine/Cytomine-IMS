package be.cytomine.multidim.hdf5.output

import be.cytomine.multidim.exceptions.CacheTooSmallException
import ch.systemsx.cisd.hdf5.HDF5Factory
import ch.systemsx.cisd.hdf5.IHDF5Reader

import java.util.concurrent.Executors

/**
 * Created by laurent on 07.01.17.
 */
class HDF5FileReader {
    public static int CACHE_MAX = 50


    private String name
    def private relatedFilenames
    private ArrayList<IHDF5Reader> readers
    private def tile_dimensions = [ ]
    def private tp
    private int dimensions
    private HashMap cache
    private int cache_size //NB this is maybe more efficient that asking cache.size()

    public HDF5FileReader(String name) {
        this.name = name
        def script = "/home/laurent/cyto_dev/Cytomine-MULTIDIM/relatedFiles.sh"
        def stringScript = "" + script + " " + name
        def retScript = stringScript.execute().text
        readers = []
        retScript = retScript.replace("\n", "")
        relatedFilenames = retScript.split(",")
     //   relatedFilenames = ["/home/laurent/cyto_dev/Cytomine-MULTIDIM/test1650.0.h5", "/home/laurent/cyto_dev/Cytomine-MULTIDIM/test1650.1.h5"]

        String meta_group = "/meta";
        int i = 0
        dimensions = 0
        relatedFilenames.each {
            readers << new HDF5Factory().openForReading(it)
            int[] meta = readers[i].int32().readArray(meta_group);
            tile_dimensions << meta
            dimensions += meta[2]
            ++i
        }
        this.tp = Executors.newFixedThreadPool(8)
        this.cache = new HashMap()
    }


    HDF5Geometry extractSpectraPixel(def arr){
        return extractSpectraPixel(arr[0], arr[1])
    }

    HDF5Geometry extractSpectraPixel(int x, int y) {
        int x_tile = x / tile_dimensions[0][0];
        int y_tile = y / tile_dimensions[0][1];
        def path =  [ "t" + x_tile + "_" + y_tile ]
        return extractSpectra(new HDF5Pixel(x,y,dimensions), path)
    }

    HDF5Geometry extractSpectraRectangle(int x, int y, int wid, int hei){
        def rec = new HDF5Rectangle(x,y,wid,hei, dimensions)
        def tiles = []
        int x_tile = x / tile_dimensions[0][0];
        int y_tile = y / tile_dimensions[0][1];
        def x_tile_end = (x + wid) / tile_dimensions[0][0]
        def y_tile_end = (y + wid) / tile_dimensions[0][1]

        (x_tile..x_tile_end).each { xx ->
            (y_tile..y_tile_end).each { yy ->
                tiles << "t" + xx + "_" + yy
            }
        }

        return extractSpectra(rec, tiles)
    }

    HDF5Geometry extractSpectraSquare(int x, int y, int size){
        return extractSpectraRectangle(x,y,size,size)
    }

    //Patharray is the array of path overlaping by the figure
    //Throws IndexOutOfBoundExceptions if bad coordinates, and
    //Warning if multiple tile requested and cache full :s
    HDF5Geometry extractSpectra(HDF5Geometry figure, def pathArray) {
        if(pathArray.size() > CACHE_MAX)
            throw new CacheTooSmallException()

        def tileConcerned = []
        pathArray.each { path ->
            if(cache.containsKey(path)){
                tileConcerned << cache.get(path)
            }
            else{
                def cacheM = CACHE_MAX
                def entry = new HDF5CubeCache(dimensions, path, tile_dimensions[0][0], tile_dimensions[0][1])
                entry.extractValues(this)
                if(!entry.isDataPresent()){
                    if(cacheM != CACHE_MAX) {//we have change the size of cache (thus a OOM has happened)
                        return extractSpectra(figure, pathArray) //rerun the stuff
                    }
                    else
                        throw new IndexOutOfBoundsException()
                }
                tileConcerned << entry

                if(cache_size > CACHE_MAX)
                    removeXLRU(cache_size - CACHE_MAX)
                cache.put(path, entry)
                cache_size++
            }
        }

        if(tileConcerned.size() !=0){
            figure.getDataFromCache(tileConcerned)
            return figure
        }
    }

    IHDF5Reader getReader(int i){
        assert i >= 0
        if(readers.size() <= i)
            return null
        return readers[i]
    }

    def getTileWidth() {
        return tile_dimensions[0][0]
    }

    def getTileHeight() {
        return tile_dimensions[0][1]
    }

    def getTileDepth(int image) {
        return tile_dimensions[image][2]
    }

    def getNumberOfImage(){
        return readers.size()
    }

    def getThreadPool(){
        return tp
    }

    def close(){
        readers.each {
            it.close()
        }
        tp.shutdown()
    }

    private void replaceLRU(){
        while(cache_size > CACHE_MAX){
            println "D"
            removeLRU()
        }
    }

    private void removeXLRU(int x){
        while(x > 0){
            removeLRU()
            x--
        }
    }

    private void removeLRU(){
        synchronized (this){
            def lru = cache.min{ it.getValue().lastUse() }
            cache.remove(lru.getKey())
            cache_size--
            println "Remove " + lru.getKey() + " from cache ("+cache_size+"/"+CACHE_MAX+")"
            System.gc()

        }
    }


    public void adaptCacheSize(){
        CACHE_MAX = cache_size - 2
        if(CACHE_MAX < 0) //TODO implement stuff without caching ?
            CACHE_MAX = 1
        println "K"
        replaceLRU()
    }
}


