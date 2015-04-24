/*
 * Copyright (c) 2009-2015. Authors: see NOTICE file.
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

class BootStrap {

    def grailsApplication

    def init = { servletContext ->
        println "Config file: "+ new File("imageserverconfig.properties").absolutePath

        println "iipImageServerBase:" + grailsApplication.config.cytomine.iipImageServerBase
        println "iipImageServerJpeg2000:" + grailsApplication.config.cytomine.iipImageServerJpeg2000
        println "iipImageServerVentana:" + grailsApplication.config.cytomine.iipImageServerVentana
        println "iipImageServerCyto:" + grailsApplication.config.cytomine.iipImageServerCyto

        Holders.config.cytomine.maxCropSize = Integer.parseInt(Holders.config.cytomine.maxCropSize+"")
    }

    def destroy = {
    }
}
