/*
 * Copyright (c) 2009-2018. Authors: see NOTICE file.
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


import be.cytomine.multidim.hdf5.output.FileReaderCache
import grails.util.Holders
import ims.DeleteImageFileJob

class BootStrap {

    def grailsApplication

    def init = { servletContext ->
        log.info "Config file: "+ new File("imageserverconfig.properties").absolutePath

        if(!grailsApplication.config.cytomine.imageServerPrivateKey) {
            throw new IllegalArgumentException("cytomine.imageServerPrivateKey must be set!")
        }
        if(!grailsApplication.config.cytomine.imageServerPublicKey) {
            throw new IllegalArgumentException("cytomine.imageServerPublicKey must be set!")
        }

        //log.info "iipImageServerBase:" + grailsApplication.config.cytomine.iipImageServerBase
        log.info "iipImageServerJpeg2000:" + grailsApplication.config.cytomine.iipImageServerJpeg2000
        log.info "iipImageServerCyto:" + grailsApplication.config.cytomine.iipImageServerCyto

        Holders.config.cytomine.maxCropSize = Integer.parseInt(Holders.config.cytomine.maxCropSize+"")
        Holders.config.cytomine.hdf5.convertBurstSize = Integer.parseInt(Holders.config.cytomine.hdf5.convertBurstSize+"")

        DeleteImageFileJob.schedule(Long.parseLong(grailsApplication.config.cytomine.deleteImageFilesFrequency), -1, [:])
    }

    def destroy = {
        log.info "Shutdown the multispectral cache"
        FileReaderCache.getInstance().shutdown()
    }
}
