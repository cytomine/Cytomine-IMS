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
import be.cytomine.client.collections.Collection
import be.cytomine.client.models.ImageServer
import be.cytomine.exception.AuthenticationException
import org.springframework.security.crypto.codec.Base64

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.servlet.http.HttpServletRequest

class CytomineService {

    def grailsApplication

    def getCytomine(String cytomineUrl) {
        String publicKey = grailsApplication.config.cytomine.ims.server.publicKey
        String privateKey = grailsApplication.config.cytomine.ims.server.privateKey
        return new Cytomine(cytomineUrl, publicKey, privateKey)
    }

    def getThisImageServer() {
        def url = grailsApplication.config.cytomine.ims.server.url
        def servers = Collection.fetch(ImageServer.class)
        for (int i = 0; i < servers.size(); i++) {
            def server = servers.get(i)
            if (server.url == url)
                return server
        }
        return null
    }

    def getAuthorizationFromRequest(def request) {
        String authorization = request.getHeader("authorization")
        if (!authorization
                || !authorization.startsWith("CYTOMINE")
                || !authorization.contains(" ")
                || !authorization.contains(":")) {
            throw new AuthenticationException("Auth failed: bad authorization")
        }
        return [
                publicKey: authorization.substring(authorization.indexOf(" ") + 1, authorization.indexOf(":")),
                signature: authorization.substring(authorization.indexOf(":") + 1)
        ]
    }

    def getMessageToSignFromRequest(def request) {
        String contentMD5Header = request.getHeader("content-MD5") ?: ""
        String dateHeader = request.getHeader("date") ?: (request.getHeader("dateFull") ?: "")
        if (dateHeader == "") {
            throw new AuthenticationException("Auth failed: no date")
        }

        if (request.getHeader("host") == null) {
            throw new AuthenticationException("Auth failed: no host")
        }

        String contentTypeHeader = ""
        if (request.getHeader("content-type")) {
            contentTypeHeader = request.getHeader("content-type")
        }
        if (request.getHeader("Content-Type")) {
            contentTypeHeader = request.getHeader("Content-Type")
        }
        if (request.getHeader("content-type-full")) {
            contentTypeHeader = request.getHeader("content-type-full")
        }
        if (contentTypeHeader == "null") {
            contentTypeHeader = ""
        }

        String queryString = (request.getQueryString() != null) ? "?${request.getQueryString()}" : ""

        def messageToSign = "${request.getMethod()}\n$contentMD5Header\n$contentTypeHeader\n$dateHeader\n"
        messageToSign += "${request.forwardURI}$queryString"
        return messageToSign
    }

    def testSignature(def privateKey, def signature, def messageToSign) {
        SecretKeySpec signingKey = new SecretKeySpec(privateKey.getBytes(), "HmacSHA1")
        Mac mac = Mac.getInstance("HmacSHA1")
        mac.init(signingKey)
        byte[] rawHmac = mac.doFinal(new String(messageToSign.getBytes(), "UTF-8").getBytes())
        byte[] signatureBytes = Base64.encode(rawHmac)
        def signatureToTest = new String(signatureBytes)

        return (signature == signatureToTest)
    }
}
