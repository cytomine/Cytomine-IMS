class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

		"/"(view:"/index")
		"500"(view:'/error')

        "/storage"(controller:"upload"){
            action = [POST:"storage"]
        }

        "/api/abstractimage/$id/associated" (controller:"openslide") {
            action = [GET:"associated"]
        }

        "/api/abstractimage/$id/associated/$label" (controller:"openslide") {
            action = [GET:"associatedImage"]
        }

        "/api/abstractimage/$id/download" (controller:"storage") {
            action = [GET:"download"]
        }
	}
}
