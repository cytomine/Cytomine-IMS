package utils

/**
 * User: lrollus
 * Date: 17/10/12
 * GIGA-ULg
 * Utility class to deals with file
 */
class ServerUtils {

    static public List<String> getServers(String conf) {
        println "conf=$conf"
        println conf.split(",").toList()
        println conf.split(",").toList().class
        return conf.split(",").toList()
    }

    static public String getServer(List<String> servers) {
        return servers.get(new Random().nextInt(servers.size()))
    }

}
