package be.cytomine.formats

import be.cytomine.exception.FormatException

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
import be.cytomine.formats.archive.ZipFormat
import be.cytomine.formats.heavyconvertable.CellSensVSIFormat
import be.cytomine.formats.heavyconvertable.DotSlideFormat
import be.cytomine.formats.heavyconvertable.OMETIFFFormat
import be.cytomine.formats.heavyconvertable.ZeissCZIFormat
import be.cytomine.formats.lightconvertable.*
import be.cytomine.formats.lightconvertable.specialtiff.*
import be.cytomine.formats.supported.*
import be.cytomine.formats.supported.digitalpathology.*
import grails.util.Holders
import org.apache.commons.lang.RandomStringUtils
import org.springframework.util.StringUtils
import utils.FilesUtils

import java.util.regex.Matcher
import java.util.regex.Pattern

public class TiffFormatDetector {

    static public getAvailableSingleFileImageFormats() {
        //check the extension and or content in order to identify the right Format
        return [
                new JPEG2000Format(),
                new ZeissCZIFormat(),
                //openslide compatibles formats
                new AperioSVSFormat(),
                new HamamatsuNDPIFormat(),
                new LeicaSCNFormat(),
                //new SakuraSVSlideFormat(),
                new PhilipsTIFFFormat(),
                new CZITIFFFormat(),
                new OMETIFFFormat(),
                //common formats
                new OMETIFFFormat(),
                new PhotoshopTIFFFormat(),
                new HuronTIFFFormat(),
                new PlanarTIFFFormat(),
                new BrokenTIFFFormat(),
                new PyramidalTIFFFormat(),
                new VentanaBIFFormat(),
                new VentanaTIFFFormat(),
                new DICOMFormat(),
                new JPEGFormat(),
                new PGMFormat(),
                new PNGFormat(),
                new BMPFormat()
        ]
    }

    static public Format getImageFormat(String filePath) {

        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = new ProcessBuilder("$tiffinfoExecutable", filePath).redirectErrorStream(true).start().text

        def formats = [new CZITIFFFormat(),
                       new OMETIFFFormat(),
                       new PhotoshopTIFFFormat(),
                       new HuronTIFFFormat(),
                       new PlanarTIFFFormat(),
                       new BrokenTIFFFormat(),
                       new PyramidalTIFFFormat()
        ]


        formats.each {
            it.absoluteFilePath = uploadedFile
        }

        def result = formats.find {
            it.detect(tiffinfo)
        }

        return result
    }

}