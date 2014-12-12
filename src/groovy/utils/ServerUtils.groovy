package utils

/**
 * User: lrollus
 * Date: 17/10/12
 * GIGA-ULg
 * Utility class to deals with file
 */
class ServerUtils {

    static public List<String> getServers(String conf) {
        return conf.split(",").toList()
    }

    static public String getServer(List<String> servers) {
        return servers.get(new Random().nextInt(servers.size()))
    }

}
