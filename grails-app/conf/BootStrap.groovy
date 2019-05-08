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


import grails.util.Holders
import ims.DeleteImageFileJob

class BootStrap {

    def grailsApplication

    def init = { servletContext ->

        log.info "Cytomine IMS configuration:"
        Holders.config.flatten().each {
            if ((it.key as String).startsWith("cytomine"))
                log.info "${it.key}: ${it.value}"
        }

        if (!Holders.config.cytomine.ims.server.url) {
            throw new IllegalArgumentException("cytomine.ims.server.url is not set !")
        }

        if (!Holders.config.cytomine.ims.server.privateKey) {
            throw new IllegalArgumentException("cytomine.ims.server.privateKey is not set!")
        }

        if (!Holders.config.cytomine.ims.server.publicKey) {
            throw new IllegalArgumentException("cytomine.ims.server.publicKey is not set!")
        }

        if (Holders.config.cytomine.ims.server.core.url && Holders.config.cytomine.ims.deleteJob.frequency) {
            DeleteImageFileJob.schedule(grailsApplication.config.cytomine.ims.deleteJob.frequency as Long, -1, [:])
        }
    }
}
