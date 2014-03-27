package cytomine.web

class SecurityFilters {
    def springSecurityService

    def filters = {
        all(uri:'/**') {
            before = {
                println "Before security"
                //tryAPIAuthentification(request,response)
            }
            after = {
                println "After security"

            }
            afterView = {

            }
        }
    }



}


