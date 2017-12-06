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


package be.cytomine.multidim.hdf5.input

import ch.systemsx.cisd.base.mdarray.MDShortArray

import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import java.awt.image.Raster
import groovy.util.logging.Log


/**
 * Created by laurent on 17.12.16.
 */

public class ExtractDataImageIO extends ExtractData{
    private String directory;
    private int dim;
    def private filenames;

    private Raster ras;


    //This is just to debug
    def benchmark = { closure ->
        def start = System.currentTimeMillis()
        closure.call()
        def now = System.currentTimeMillis()
        now - start
    }


    public ExtractDataImageIO(String dirr, def filenames){
        this.filenames = filenames;
        this.dim = filenames.size();
        this.directory = dirr;
        try {
            String filename = directory + "/"  + filenames[0];
            BufferedImage bf = ImageIO.read(new File(filename));
            ras = bf.getData();
        } catch (IOException e) {
            log.info directory + "/" + filenames[0] + " Not found"
        }
    }


    public int getImageWidth(){
        return ras.getWidth();
    }

    public int getImageHeight(){
        return ras.getHeight();
    }

    public int getImageDepth(){
        return this.dim;
    }


    public void getImage(int i){
        try {
            String filename = directory + "/"  + filenames[i];
            BufferedImage bf = ImageIO.read(new File(filename));
            ras = bf.getData();

        } catch (IOException e) {
            log.info filenames[i] + " not found"
        }

    }


    public MDShortArray extract2DCube(int startX, int startY, int wid, int hei, int depth){
        long[] dims = [wid, hei, depth];
        MDShortArray result = new MDShortArray(dims);
        return extract2DCube(startX, startY, 0, wid, hei, result)
    }

    public MDShortArray extract2DCube(int startX, int startY, int dim, int wid, int hei, MDShortArray base){
     //   println "Sx "  + startX + " Sy " + startY + " d " + dim  +" w " + wid + " hei " +hei
        if(startX + wid >= getImageWidth()){
            wid = getImageWidth() - startX
        }
        if(startY + hei >= getImageHeight()){
            hei = getImageHeight() - startY
        }
        int[] chapeau = new int[256*256]
        int[] v=  ras.getSamples(startX,startY,wid, hei, 0, chapeau)
        int id = 0
        v.each { val->
                base.set((short)val, (int) (id / 256), id % 256, dim)
                ++id
            }
        return base as MDShortArray
    }

}
