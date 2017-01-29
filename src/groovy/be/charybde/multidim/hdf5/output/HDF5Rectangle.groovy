package be.charybde.multidim.hdf5.output

import ch.systemsx.cisd.base.mdarray.MDShortArray

import java.util.concurrent.Callable
import java.util.concurrent.Future

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

    def String getCSV(){
        return getValues().toString()
    }
}
