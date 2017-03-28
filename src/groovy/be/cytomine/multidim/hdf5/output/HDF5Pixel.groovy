/*
 * Copyright (c) 2009-2017. Authors: see NOTICE file.
 *
 * Licensed under the GNU Lesser General Public License, Version 2.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
