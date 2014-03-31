class UrlMappings {

	static mappings = {
		"/$controller/$action?/$id?"{
			constraints {
				// apply constraints here
			}
		}

		"/"(view:"/index")
		"500"(view:'/error')

        "/upload"(controller:"upload"){
            action = [POST:"upload"]
        }

        "/api/abstractimage/$id/associated" (controller:"openSlide") {
            action = [GET:"associated"]
        }

        "/api/abstractimage/$id/associated/$label" (controller:"openSlide") {
            action = [GET:"label"]
        }

        "/api/abstractimage/$id/download" (controller:"openSlide") {
            action = [GET:"download"]
        }
	}
}
