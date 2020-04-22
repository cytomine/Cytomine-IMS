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

        /* Storage controller */

        "/upload"(controller:"storage"){
            action = [POST:"upload"]
        }

//        "/uploadCrop" (controller:"image") {
//            action = [POST:"uploadCrop"]
//        }

        "/storage/size.$format"(controller:"storage"){
            action = [GET:"size"]
        }


        /* Image controller */

        "/image/associated.$format" (controller:"image") {
            action = [GET:"associated"]
        }

        "/image/nested.$format" (controller:"image") {
            action = [GET:"nested", POST:"nested"]
        }

        "/image/properties.$format" (controller:"image") {
            action = [GET:"properties"]
        }

        "/image/download" (controller: "image") {
            action = [GET: "download"]
        }

        /* Slice controller */

        "/slice/thumb.$format" (controller:"slice") {
            action = [GET:"thumb", POST:"thumb"]
        }

        "/slice/crop.$format" (controller:"slice") {
            action = [GET:"crop", POST:"crop"]
        }

        "/slice/tile" (controller: "slice") {
            action = [GET:"tile"]
        }

        "/slice/histogram.$format" (controller: "slice") {
            action = [GET:"histogram"]
        }

        /* Profile controller */

        "/profile.$format" (controller: "profile") {
            action = [POST: "computeProfile", GET: "extractProfile"]
        }

        "/profile/stats.$format" (controller: "profile") {
            action = [POST: "statsProfile", GET: "statsProfile"]
        }

        "/profile/min-projection.$format" (controller: "profile") {
            action = [POST: "minProjection", GET: "minProjection"]
        }

        "/profile/max-projection.$format" (controller: "profile") {
            action = [POST: "maxProjection", GET: "maxProjection"]
        }

        "/profile/average-projection.$format" (controller: "profile") {
            action = [POST: "averageProjection", GET: "averageProjection"]
        }
	}
}
