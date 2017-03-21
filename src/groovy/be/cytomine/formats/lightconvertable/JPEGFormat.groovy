package be.cytomine.formats.lightconvertable

import be.cytomine.exception.MiddlewareException

/*
 * Copyright (c) 2009-2016. Authors: see NOTICE file.
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

import grails.util.Holders
import org.openslide.OpenSlide
import utils.ServerUtils

/**
 * Created by stevben on 22/04/14.
 */
public class JPEGFormat extends CommonFormat {

    public JPEGFormat () {
        extensions = ["jpg", "jpeg"]
        IMAGE_MAGICK_FORMAT_IDENTIFIER = "JPEG"
        //mimeType = "image/jpeg"
        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerBase)
    }

    boolean detect() {
        boolean isJPEG = super.detect()
        if (isJPEG) { //check if not MRXS (fake JPEG)
            File slideFile = new File(absoluteFilePath)
            if (slideFile.canRead()) {
                try {
                    return !OpenSlide.detectVendor(slideFile)
                } catch (java.io.IOException e) {
                    //Not a file that OpenSlide can recognize
                    return true
                }
            } else {
                throw new MiddlewareException("ERROR cannot reading JPEG file")
            }
        }


    }
}
