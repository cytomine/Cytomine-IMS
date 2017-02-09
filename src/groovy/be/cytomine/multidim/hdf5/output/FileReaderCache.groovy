package be.cytomine.multidim.hdf5.output

import java.util.concurrent.Executors

/**
 * Created by laurent on 17.01.17.
 */
class FileReaderCache {

    private static FileReaderCache singleton
    private HashMap<String, HDF5FileReader> cache
    private def threadpool //Nb maybe we should use only one tp for the filereaders (this one)

    private FileReaderCache(){
        this.cache = new HashMap<String, HDF5FileReader>()
        this.threadpool =  Executors.newFixedThreadPool(8)

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

    public void shutdown(){
        cache.each {
            it.close()
        }
    }


}
