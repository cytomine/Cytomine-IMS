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
/**
 * Created by laurent on 08.01.17.
 */
class HDF5Rectangle implements HDF5Geometry{
    private int x, y, dim, wid, hei


    HDF5Rectangle(int x, int y, int wid, int hei, int dim) {
        this.x = x
        this.y = y
        this.dim = dim
        this.wid = wid
        this.hei = hei
    }

    @Override
    def getDataFromCache(def array) {
        def data = []
        def xStart, xEnd, yStart, yEnd
        array.each{ cache ->
            xStart = [x, cache.getXStart()].max()
            yStart = [y, cache.getYStart()].max()
            xEnd = [x+wid-1, cache.getXEnd()].min()
            yEnd = [y+hei-1, cache.getYEnd()].min()

            for(int i = xStart; i < xEnd; ++i){
                for(int j = yStart; j < yEnd; ++j){
                    data << [ [i,j],  cache.getPixelInCache(i,j)  ]
                }
            }

        }

        setData(data)
    }


    @Override
    def getDim() {
        return [x,y,dim,wid,hei]
    }

}
