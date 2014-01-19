package cytomine.web

import net.jr.fastcgi.impl.FastCGIHandler
import net.jr.fastcgi.impl.FastCGIHandlerFactory

class ImageServerService {

    def start() {
        log.info("Start IIP instances...")

        /*String fcgiPath = System.properties['base.dir'] + "/fcgi-bin/iipsrv.fcgi"
        String command = "$fcgiPath --bind 127.0.0.1:9000"
        println command
        command.execute()*/

    }

    def stop() {
        log.info("Stop IIP instances...")


    }
}
