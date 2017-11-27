package be.cytomine.server

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

import be.cytomine.client.Cytomine
import be.cytomine.exception.AuthenticationException
import org.springframework.security.crypto.codec.Base64

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.HttpServletRequest

class CytomineService {

    def grailsApplication

    def getCytomine(String cytomineUrl) {
        String publicKey = grailsApplication.config.cytomine.imageServerPublicKey
        String privateKey = grailsApplication.config.cytomine.imageServerPrivateKey
        return new Cytomine(cytomineUrl, publicKey, privateKey, "./", false)
    }

    public def tryAPIAuthentification(def cytomineUrl,def ISPubKey, def ISPrivKey, HttpServletRequest request) {

        log.info "tryAPIAuthentification"
        log.info "cytomineUrl=$cytomineUrl"
        log.info "ISPubKey=$ISPubKey"
        log.info "ISPrivKey=$ISPrivKey"

        String authorization = request.getHeader("authorization")
        log.info "authorization=$authorization"
        if (request.getHeader("dateFull") == null && request.getHeader("date") == null) {
            log.info "Auth failed: no date"
            throw new AuthenticationException("Auth failed: no date")
        }
        if (request.getHeader("host") == null) {
            log.info "Auth failed: no host"
            throw new AuthenticationException("Auth failed: no host")
        }
        if (authorization == null) {
            log.info "Auth failed: no authorization"
            throw new AuthenticationException("Auth failed: no authorization")
        }
        if (!authorization.startsWith("CYTOMINE") || !authorization.indexOf(" ") == -1 || !authorization.indexOf(":") == -1) {
            log.info "Auth failed: bad authorization"
            throw new AuthenticationException("Auth failed: bad authorization")
        }
        request.getHeaderNames().each {
            log.info it + "=" + request.getHeader(it)
        }


        String content_md5 = (request.getHeader("content-MD5") != null) ? request.getHeader("content-MD5") : ""
        //println "content_md5=" + content_md5
        String content_type = (request.getHeader("content-type") != null) ? request.getHeader("content-type") : ""
        content_type = (request.getHeader("Content-Type") != null) ? request.getHeader("Content-Type") : content_type
        content_type = (request.getHeader("content-type-full") != null) ? request.getHeader("content-type-full") : content_type
        if(content_type=="null") {
            content_type =""
        }

        //println "content_type=" + content_type
        String date = (request.getHeader("date") != null) ? request.getHeader("date") : ""
        date = (request.getHeader("dateFull") != null) ? request.getHeader("dateFull") : date

        log.info "finalDate=" + date
        log.info "forwardURI=" + request.forwardURI
        String canonicalHeaders = request.getMethod() + "\n" + content_md5 + "\n" + content_type + "\n" + date + "\n"
        //println "canonicalHeaders=" + canonicalHeaders
        String canonicalExtensionHeaders = ""
        String queryString = (request.getQueryString() != null) ? "?" + request.getQueryString() : ""
        String path = request.forwardURI //original URI Request
        String canonicalResource = path + queryString
        //println "canonicalResource=" + canonicalResource
        String messageToSign = canonicalHeaders + canonicalExtensionHeaders + canonicalResource
        //println "messageToSign=$messageToSign"
        String accessKey = authorization.substring(authorization.indexOf(" ") + 1, authorization.indexOf(":"))
        //println "accessKey=" + accessKey
        String authorizationSign = authorization.substring(authorization.indexOf(":") + 1)
        //println "authorizationSign=" + authorizationSign

        log.info "content_md5=$content_md5"
        log.info "content_type=$content_type"
        log.info "date=$date"
        log.info "queryString=$queryString"
        log.info "path=$path"
        log.info "method=${request.getMethod()}"

        log.info "accessKey=$accessKey"

        log.info "Connection Cytomine: $cytomineUrl $ISPubKey $ISPrivKey"

        Cytomine cytomine = new Cytomine(cytomineUrl, ISPubKey,ISPrivKey)

        log.info "cytomine.getUser($accessKey)"

        def retrieveUser = cytomine.getUser(accessKey)
        if (retrieveUser?.id==null) {
            log.info "User not found with key $accessKey!"
            throw new AuthenticationException("Auth failed: User not found with key $accessKey! May be ImageServer user is not an admin!")
        }

        long id = retrieveUser.id

        String privatekey = cytomine.getKeys(accessKey).get("privateKey")
        log.info "Privatekey=$privatekey"
        log.info "PublicKey=$accessKey"

        SecretKeySpec signingKey = new SecretKeySpec(privatekey.getBytes(), "HmacSHA1")
        //println "signingKey=" + signingKey
        // get an hmac_sha1 Mac instance and initialize with the signing key
        Mac mac = Mac.getInstance("HmacSHA1")
        //println "mac=" + mac
        mac.init(signingKey)
        // compute the hmac on input data bytes
        byte[] rawHmac = mac.doFinal(new String(messageToSign.getBytes(), "UTF-8").getBytes())
        //println "rawHmac=" + rawHmac.length
        // base64-encode the hmac
        byte[] signatureBytes = Base64.encode(rawHmac)
        //println "signatureBytes=" + signatureBytes.length
        def signature = new String(signatureBytes)
        //println "signature=" + signature

        log.info "authorizationSign=$authorizationSign"
        log.info "signature=$signature"
        if (authorizationSign == signature) {
            log.info "AUTH TRUE"
            return ["id": id, "privateKey": privatekey, "publicKey": accessKey]
        } else {
            log.info "AUTH FALSE"
            throw new AuthenticationException("Auth failed")
        }
    }
}
