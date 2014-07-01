package cytomine.web

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 19/08/11
 * Time: 9:49
 * To change this template use File | Settings | File Templates.
 */

class RequestFilters {

    def springSecurityService

    def filters = {
        //all(uri:'/api/**') {
        all(uri:'/**') {
            before = {
                request.currentTime = System.currentTimeMillis()
                log.info  request.getRequestURI()
                log.info  request.getRequestURL()
                log.info  request.getContextPath()
                log.info  request.getPathInfo()
                log.info  request.getServletPath()
                log.info params
                log.info controllerName+"."+actionName
            }
            after = {}
            afterView = {
                log.info controllerName+"."+actionName + " Request took ${System.currentTimeMillis()-request.currentTime}ms"
            }
        }
    }
}
