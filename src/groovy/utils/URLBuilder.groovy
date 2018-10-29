package utils

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

class URLBuilder {
    def host
    def charset
    def parameters = []

    URLBuilder(host, charset) {
        this.charset = charset
        this.host = host
    }

    URLBuilder(host) {
        charset = "UTF-8"
        this.host = host
    }

    def addParameter(param, value, encode=false) {
        if (encode)
            parameters << [key: param, value: URLEncoder.encode(value, charset)]
        else
            parameters << [key: param, value: value]
    }

    String toString() {
        if (parameters.isEmpty())
            return host
        else
            return host + "?" + parameters.collect {"${it.key}=${it.value}"}.join("&")
    }
}
