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

        "/api/image/$id/associated" (controller:"image") {
            action = [GET:"associated"]
        }

        "/api/image/$id/associated/$label" (controller:"image") {
            action = [GET:"label"]
        }
	}
}
