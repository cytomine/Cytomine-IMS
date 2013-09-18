package cytomine.web

import be.cytomine.client.Cytomine
import be.cytomine.client.models.User

import org.springframework.security.crypto.codec.Base64

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class APIAuthentificationFilters implements javax.servlet.Filter {

    void init(FilterConfig filterConfig) {

    }

    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        tryAPIAuthentification(request, response)
        chain.doFilter(request, response)
    }

    void destroy() {}

    /**
     * http://code.google.com/apis/storage/docs/reference/v1/developer-guidev1.html#authentication
     */
    private boolean tryAPIAuthentification(HttpServletRequest request, HttpServletResponse response) {
        println "tryAPIAuthentification"
        String authorization = request.getHeader("authorization")
        if (request.getHeader("date") == null) {
            return false
        }
        if (request.getHeader("host") == null) {
            return false
        }
        if (authorization == null) {
            return false
        }
        if (!authorization.startsWith("CYTOMINE") || !authorization.indexOf(" ") == -1 || !authorization.indexOf(":") == -1) {
            return false
        }
        try {

            String content_md5 = (request.getHeader("content-MD5") != null) ? request.getHeader("content-MD5") : ""
//            println "content_md5="+content_md5
            String content_type = (request.getHeader("content-type") != null) ? request.getHeader("content-type") : ""
            content_type = (request.getHeader("Content-Type") != null) ? request.getHeader("Content-Type") : content_type
//            println "content_type="+content_type
            String date = (request.getHeader("date") != null) ? request.getHeader("date") : ""
//            println "date="+date
            String canonicalHeaders = request.getMethod() + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n"
//            println "canonicalHeaders="+canonicalHeaders
            String canonicalExtensionHeaders = ""
            String queryString = (request.getQueryString() != null) ? "?" + request.getQueryString() : ""
            String path = request.forwardURI //original URI Request
            String canonicalResource = path + queryString
            String messageToSign = canonicalHeaders + canonicalExtensionHeaders + canonicalResource
            String accessKey = authorization.substring(authorization.indexOf(" ") + 1, authorization.indexOf(":"))
            String authorizationSign = authorization.substring(authorization.indexOf(":") + 1)

            //TODO: ask user with public key
            //TODO: pubKey = a50f6f5d-1bcb-4cca-ac37-9bbf8581f25e, privKey = 278c5d52-396b-4036-b535-d541652edffa
            Cytomine cytomine = new Cytomine("http://localhost:8080","a50f6f5d-1bcb-4cca-ac37-9bbf8581f25e","278c5d52-396b-4036-b535-d541652edffa","./")
            User user = cytomine.getKeys(accessKey)

            if(!user) {
                println "User not found with key $accessKey!"
                return false
            }

            //TODO: get its private key
            String key = user.get("privateKey")

            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1")
            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance("HmacSHA1")
            mac.init(signingKey)
            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(new String(messageToSign.getBytes(), "UTF-8").getBytes())

            // base64-encode the hmac
            byte[] signatureBytes = Base64.encode(rawHmac)
            def signature = new String(signatureBytes)
            if (authorizationSign == signature) {
                println "AUTH TRUE"
                return true
            } else {
                println "AUTH FALSE"
                return false
            }
        } catch (Exception e) {
            e.printStackTrace()
            return false
        }
        return false
    }

    def filters = {
        all(uri:'/**') {
            before = {
            }
            after = {

            }
            afterView = {

            }
        }
    }

}
