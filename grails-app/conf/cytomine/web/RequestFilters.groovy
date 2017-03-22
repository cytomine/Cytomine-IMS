package cytomine.web

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

class RequestFilters {

    def springSecurityService

    def filters = {
        //all(uri:'/api/**') {
        all(uri:'/**') {
            before = {
                /*request.currentTime = System.currentTimeMillis()
                log.info  request.getRequestURI()
                log.info  request.getRequestURL()
                log.info  request.getContextPath()
                log.info  request.getPathInfo()
                log.info  request.getServletPath()
                log.info params
                log.info controllerName+"."+actionName*/
            }
            after = {}
            afterView = {
                //log.info controllerName+"."+actionName + " Request took ${System.currentTimeMillis()-request.currentTime}ms"
            }
        }
    }
}
