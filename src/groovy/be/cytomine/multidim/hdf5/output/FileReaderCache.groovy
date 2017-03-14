/*
 * Copyright (c) 2009-2017. Authors: see NOTICE file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
