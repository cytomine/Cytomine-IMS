package be.charybde.multidim.hdf5.output

/**
 * Created by laurent on 17.01.17.
 */
class FileReaderCache {

    private static FileReaderCache singleton
    private HashMap<String, HDF5FileReader> cache

    private FileReaderCache(){
        this.cache = new HashMap<>()
    }

    public static FileReaderCache getInstance(){
        if(singleton == null){
            singleton = new FileReaderCache()
        }
        return singleton
    }

    public HDF5FileReader getReader(def name){
        if(cache.containsKey(name)){
            return cache.get(name)
        }
        def reader = new HDF5FileReader(name)
        cache.put(name, reader)
        return reader
    }
}
