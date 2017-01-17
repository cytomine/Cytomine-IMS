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
            if (cache.getXStart() < x) // xstart = max ( x, cache.getXstart())
                xStart = x
            else
                xStart = cache.getXStart()
            if(cache.getYStart() < y)
                yStart = y
            else
                yStart = cache.getYStart()
            if(cache.getXEnd() < x + wid - 1)
                xEnd = cache.getXEnd()
            else
                xEnd = x + wid - 1
            if(cache.getYEnd() < y + hei - 1)
                yEnd = cache.getYEnd()
            else
                yEnd = y + hei - 1

            xStart.upto(xEnd, { i->
                yStart.upto(yEnd, { j->
                    data << [ [i,j],  cache.getPixelInCache(i,j)  ]
                })
            })

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
