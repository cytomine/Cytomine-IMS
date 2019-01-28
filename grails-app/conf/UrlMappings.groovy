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

class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

		"/"(view:"/index")
		"500"(view:'/error')

        "/upload"(controller:"storage"){
            action = [POST:"upload"]
        }

        "/uploadCrop" (controller:"image") {
            action = [POST:"uploadCrop"]
        }

        "/download"(controller:"storage"){
            action = [GET : "download"]
        }

        "/image/associated.$format" (controller:"image") {
            action = [GET:"associated"]
        }

        "/image/nested.$format" (controller:"image") {
            action = [GET:"nested", POST:"nested"]
        }

        "/image/properties.$format" (controller:"image") {
            action = [GET:"properties"]
        }

        "/image/thumb.$format" (controller:"image") {
            action = [GET:"thumb", POST:"thumb"]
        }

        "/image/crop.$format" (controller:"image") {
            action = [GET:"crop", POST:"crop"]
        }

        "/storage/size.$format"(controller:"storage"){
            action = [GET:"size"]
        }

        "/multidim/pixel.$format" (controller:"multiDim"){
            action = [GET:"getSpectraPixel"]
        }

        "/multidim/rectangle.$format" (controller:"multiDim"){
            action = [GET:"getSpectraRectangle"]
        }

        "/multidim/convert.$format" (controller: "multiDim"){
            action = [POST: "convertListToHdf5"]
        }

        "/image/tile" (controller: "image") {
            action = [GET:"tileZoomify"] //tileIIP
        }
	}
}
