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

        "/api/abstractimage/$id/associated" (controller:"abstractImage") {
            action = [GET:"associated"]
        }

        "/api/abstractimage/$id/associated/$label" (controller:"abstractImage") {
            action = [GET:"label"]
        }

        "/api/abstractimage/$id/download" (controller:"abstractImage") {
            action = [GET:"download"]
        }
	}
}
