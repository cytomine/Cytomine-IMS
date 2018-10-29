package be.cytomine.server

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

import be.cytomine.client.Cytomine
import be.cytomine.client.models.Storage
import be.cytomine.client.models.UploadedFile
import be.cytomine.exception.DeploymentException
import utils.ProcUtils

class DeployImagesService {

    def fileSystemService

    static transactional = true

    //could be removed and use an standard MV function by passing two directories...
    UploadedFile copyUploadedFile(Cytomine cytomine, String uploadedFilePath, uploadedFile, Collection<Storage> storages) {
        def localFile = uploadedFilePath

        storages.each { storage ->
            def destFilename = storage.getStr("basePath") + File.separator + uploadedFile.getStr("filename")
            if(!new File(new File(destFilename).parent).exists()) {
                fileSystemService.makeLocalDirectory(new File(destFilename).parent)
            }

            def command = """mv "$localFile" "$destFilename" """
            log.info "Command=$command"
            ProcUtils.executeOnShell(command)

            if(!new File(destFilename).exists()) {
                log.error new File(destFilename).absolutePath + " created = " + new File(destFilename).exists()
                throw new DeploymentException(new File(destFilename).absolutePath + " is not created! ")
            }

        }
        return uploadedFile
    }
}
