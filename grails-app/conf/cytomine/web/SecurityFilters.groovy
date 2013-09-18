package cytomine.web

import be.cytomine.client.Cytomine
import be.cytomine.client.models.User
import org.springframework.security.crypto.codec.Base64

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class SecurityFilters {
    def springSecurityService

    def dependsOn = [APIAuthentificationFilters]

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


