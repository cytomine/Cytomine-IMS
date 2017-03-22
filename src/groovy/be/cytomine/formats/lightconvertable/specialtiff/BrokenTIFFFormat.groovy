package be.cytomine.formats.lightconvertable.specialtiff

import org.springframework.util.StringUtils

import java.util.regex.Matcher
import java.util.regex.Pattern

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


/**
 * Created by hoyoux on 16.02.15.
 */
class BrokenTIFFFormat extends TIFFFormat{

    public BrokenTIFFFormat () {
        extensions = ["tif", "tiff"]
    }

    public boolean detect() {
        String tiffinfo = getTiffInfo()

        if (tiffinfo.contains("not a valid IFD offset.")) return true;

        int nbTiffDirectory = StringUtils.countOccurrencesOf(tiffinfo, "TIFF Directory")
        int nbWidth = StringUtils.countOccurrencesOf(tiffinfo, "Image Width:")
        if(nbTiffDirectory  == 2 && nbWidth < 2) return true

        if(nbTiffDirectory  == 1 && tiffinfo.contains("Tile Width")){

            int imWidth = -1;
            int imHeight = -1;
            int tileWidth = -1;
            int tileHeight = -1;

            Pattern pattern = Pattern.compile("Image Width: (\\d)+");
            Matcher matcher = pattern.matcher(tiffinfo);
            if (matcher.find()){
                imWidth = Integer.parseInt(tiffinfo.substring("Image Width: ".length()+matcher.start(),matcher.end()))
            }
            pattern = Pattern.compile("Tile Width: (\\d)+");
            matcher = pattern.matcher(tiffinfo);
            if (matcher.find())
            {
                tileWidth = Integer.parseInt(tiffinfo.substring("Tile Width: ".length()+matcher.start(),matcher.end()))
            }

            if(tileWidth > -1 && imWidth > tileWidth) return true;

            pattern = Pattern.compile("Image Length: (\\d)+");
            matcher = pattern.matcher(tiffinfo);
            if (matcher.find()){
                imHeight = Integer.parseInt(tiffinfo.substring("Image Length: ".length()+matcher.start(),matcher.end()))
            }
            pattern = Pattern.compile("Tile Length: (\\d)+");
            matcher = pattern.matcher(tiffinfo);
            if (matcher.find())
            {
                tileHeight = Integer.parseInt(tiffinfo.substring("Tile Length: ".length()+matcher.start(),matcher.end()))
            }

            if(tileHeight > -1 && imHeight > tileHeight) return true;
        }


        return false
    }
}
