package be.cytomine.multidim

import be.charybde.multidim.hdf5.output.FileReaderCache
import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType


@RestApi(name = "Multi dimentional services", description = "Methods for getting the spectra of geormetric forms")
class MultiDimController {


    @RestApiMethod(description="Get the spectra of  a pixel", extensions = [".json"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType= RestApiParamType.QUERY, description="The absolute path of a multispectral image"),
            @RestApiParam(name="x", type="int", paramType= RestApiParamType.QUERY, description="The X coordinate" ),
            @RestApiParam(name="y", type="int", paramType= RestApiParamType.QUERY, description="The y coordinate" )

    ])
    def pxl(){
        String name = params.fif
        def coo = [Integer.parseInt(params.x) , Integer.parseInt(params.y)]
        def aMap = new HashMap()
        aMap.put("pxl", coo)
        def read = FileReaderCache.getInstance().getReader(name)
        def spectra = read.extractSpectraPixel(coo)
        aMap.put("spectra", spectra.getValues())


        render aMap as JSON
    }


    @RestApiMethod(description="Get the spectra of  a rectangle", extensions = [".json"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType= RestApiParamType.QUERY, description="The absolute path of a multispectral image"),
            @RestApiParam(name="x", type="int", paramType= RestApiParamType.QUERY, description="The X coordinate" ),
            @RestApiParam(name="y", type="int", paramType= RestApiParamType.QUERY, description="The y coordinate" ),
            @RestApiParam(name="w", type="int", paramType= RestApiParamType.QUERY, description="The width of the rectangle" ),
            @RestApiParam(name="h", type="int", paramType= RestApiParamType.QUERY, description="The height of the rectangle" )

    ])
    def rect(){
        String name = params.fif
        def x = Integer.parseInt(params.x)
        def y = Integer.parseInt(params.y)
        def w = Integer.parseInt(params.w)
        def h = Integer.parseInt(params.h)

        def aMap = new HashMap()
        def read = FileReaderCache.getInstance().getReader(name)
        def spectra = read.extractSpectraRectangle(x,y,w,h)
        def i = 0

        spectra.getValues().each { pxl ->
            def bMap = new HashMap()
            bMap.put("pixel", pxl[0])
            bMap.put("spectra", pxl[1])
            aMap.put(i, bMap)
            ++i
        }

        render aMap as JSON
    }

}
