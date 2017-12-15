package be.cytomine.multidim

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

import grails.converters.JSON
import org.restapidoc.annotation.RestApi
import org.restapidoc.annotation.RestApiMethod
import org.restapidoc.annotation.RestApiParam
import org.restapidoc.annotation.RestApiParams
import org.restapidoc.pojo.RestApiParamType


@RestApi(name = "Multidimensional services", description = "Methods to obtain the spectrum of a multidimensional image")
class MultiDimController {
    def multiDimService

    @RestApiMethod(description="Create a multidimensional HDF5 file", extensions = ["json"])
    @RestApiParams(params=[
            @RestApiParam(name="files", type="list", paramType= RestApiParamType.QUERY, description="A list of image to convert"),
            @RestApiParam(name="dest", type="String", paramType= RestApiParamType.QUERY, description="The destination path of the HDF5 file"),
            @RestApiParam(name="bpc", type="int", paramType= RestApiParamType.QUERY, description="The number of bit per channel in the images")
    ])
    def convertListToHdf5(){
        def destination = params.dest
        def files = params.files
        def bpc = params.int('bpc', 8)

        Thread.start {
            multiDimService.convert(destination, files, bpc)
        }

        def data = [response: "Conversion launched"]
        render data as JSON
    }


    @RestApiMethod(description="Get the spectrum of a pixel", extensions = ["json"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType= RestApiParamType.QUERY, description="The absolute path of a multidimensional image", required=true),
            @RestApiParam(name="x", type="int", paramType= RestApiParamType.QUERY, description="The x coordinate (0 is left)", required=true),
            @RestApiParam(name="y", type="int", paramType= RestApiParamType.QUERY, description="The y coordinate (0 is top)", required=true),
            @RestApiParam(name="minChannel", type="int", paramType=RestApiParamType.QUERY, description="The minimum channel", required=false),
            @RestApiParam(name="maxChannel", type="int", paramType=RestApiParamType.QUERY, description="The maximum channel", required=false),
    ])
    def getSpectraPixel(){
        def fif = params.fif
        def x = params.int('x')
        def y = params.int('y')
        def minChannel = params.int('minChannel', 0)
        def maxChannel = params.int('maxChannel', Integer.MAX_VALUE)

        def pixel, spectrum
        (pixel, spectrum) = multiDimService.pixelSpectrum(fif, x, y, minChannel, maxChannel)

        def data = [:]
        data << [pxl: pixel]
        data << [spectra: spectrum]

        render data as JSON
    }


    @RestApiMethod(description="Get the spectrum of a rectangle", extensions = ["json"])
    @RestApiParams(params=[
            @RestApiParam(name="fif", type="String", paramType= RestApiParamType.QUERY, description="The absolute path of a multidimensional image"),
            @RestApiParam(name="x", type="int", paramType= RestApiParamType.QUERY, description="The x coordinate of top-left corner (0 is left)"),
            @RestApiParam(name="y", type="int", paramType= RestApiParamType.QUERY, description="The y coordinate of top-left corner (0 is top)"),
            @RestApiParam(name="w", type="int", paramType= RestApiParamType.QUERY, description="The width of the rectangle"),
            @RestApiParam(name="h", type="int", paramType= RestApiParamType.QUERY, description="The height of the rectangle"),
            @RestApiParam(name="minChannel", type="int", paramType=RestApiParamType.QUERY, description="The minimum channel", required=false),
            @RestApiParam(name="maxChannel", type="int", paramType=RestApiParamType.QUERY, description="The maximum channel", required=false),
    ])
    def getSpectraRectangle(){
        def fif = params.fif
        def x = params.int('x')
        def y = params.int('y')
        def width = params.int('w')
        def height = params.int('h')
        def minChannel = params.int('minChannel', 0)
        def maxChannel = params.int('maxChannel', Integer.MAX_VALUE)

        def data = []
        multiDimService.rectangleSpectrum(fif, x, y, width, height, minChannel, maxChannel).each {
            data << [pxl: it[0], spectra: it[1]]
        }

        def json = [collection: data]
        render json  as JSON
    }
}
