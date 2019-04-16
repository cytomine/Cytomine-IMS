package utils

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
