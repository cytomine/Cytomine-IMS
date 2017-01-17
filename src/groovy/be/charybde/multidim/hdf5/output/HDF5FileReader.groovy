package be.charybde.multidim.hdf5.output

import ch.systemsx.cisd.hdf5.HDF5Factory
import ch.systemsx.cisd.hdf5.IHDF5Reader
import ncsa.hdf.hdf5lib.exceptions.HDF5SymbolTableException

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Created by laurent on 07.01.17.
 */
class HDF5FileReader {
    private String name
    def private relatedFilenames
    private ArrayList<IHDF5Reader> readers
    private int tile_width, tile_height, tile_depth
    def private tp
    private int dimensions
    private HashMap cache

    public HDF5FileReader(String name) {
        this.name = name
        def script = "/home/laurent/cyto_dev/Cytomine-MULTIDIM/relatedFiles.sh"
        def stringScript = "" + script + " " + name
        def retScript = stringScript.execute().text
        readers = []
        retScript = retScript.replace("\n", "")
        relatedFilenames = retScript.split(",")
     //   relatedFilenames = ["/home/laurent/cyto_dev/Cytomine-MULTIDIM/test1650.0.h5", "/home/laurent/cyto_dev/Cytomine-MULTIDIM/test1650.1.h5"]
        relatedFilenames.each {
            readers << new HDF5Factory().openForReading(it)
        }

        String meta_group = "/meta";
        int[] meta = readers[0].int32().readArray(meta_group);
        tile_width = meta[0]
        tile_height = meta[1]
        tile_depth = meta[2]
        dimensions = relatedFilenames.size() * tile_depth //Note this is only ok if we have one file per tile depth
        this.tp = Executors.newFixedThreadPool(8)
        this.cache = new HashMap()
    }


    HDF5Geometry extractSpectraPixel(def arr){
        return extractSpectraPixel(arr[0], arr[1])
    }

    HDF5Geometry extractSpectraPixel(int x, int y) {
        int x_tile = x / tile_width;
        int y_tile = y / tile_height;
        def path =  [ "t" + x_tile + "_" + y_tile ]
        return extractSpectra(new HDF5Pixel(x,y,dimensions), path)
    }

    HDF5Geometry extractSpectraRectangle(int x, int y, int wid, int hei){
        def rec = new HDF5Rectangle(x,y,wid,hei, dimensions)
        def tiles = []
        int x_tile = x / tile_width;
        int y_tile = y / tile_height;
        def x_tile_end = (x + wid) / tile_width
        def y_tile_end = (y + wid) / tile_height

        // TODO parall
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
    //Throws IndexOutOfBoundExceptions if shit happens
    HDF5Geometry extractSpectra(HDF5Geometry figure, def pathArray) {
        def tileConcerned = []
        pathArray.each { path ->
            if(cache.containsKey(path)){
                tileConcerned << cache.get(path)
            }
            else{
                def entry = new HDF5TileCache(dimensions, path)
                entry.extractValues(this)
                if(!entry.isDataPresent())
                    throw new IndexOutOfBoundsException()
                cache.put(path, entry)
                tileConcerned << entry
            }
        }
        figure.getDataFromCache(tileConcerned)
        return figure
    }

    IHDF5Reader getReader(int i){
        assert i >= 0
        if(readers.size() <= i)
            return null
        return readers[i]
    }

    def getTileWidth() {
        return tile_width
    }

    def getTileHeight() {
        return tile_height
    }

    def getTileDepth() {
        return tile_depth
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
}


