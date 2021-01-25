package utils

/*
 * Copyright (c) 2009-2019. Authors: see NOTICE file.
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

class HttpUtils {

    public static String makeUrl(String host, String uri, def parameters, String protocol) {
        if (!(host.startsWith("http://") || host.startsWith("https://"))) {
            host = protocol + host
        }

        String query = concatenateParameters(parameters)
        return "$host$uri?$query"
    }

    public static String makeUrl(String url, def parameters) {
        if (!url.endsWith("?")) url += "?"
        return url + concatenateParameters(parameters)
    }

    public static String concatenateParameters(def parameters) {
        String query = parameters.findAll{it.value != null}.collect { key, value ->
            if (value instanceof String)
                value = encode(value)
            "$key=$value"
        }.join("&")
        return query
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, "UTF-8")
    }
}
