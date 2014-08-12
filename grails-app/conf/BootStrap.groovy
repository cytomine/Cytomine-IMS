class BootStrap {

    def grailsApplication

    def init = { servletContext ->
        println "Config file: "+ new File("imageserverconfig.properties").absolutePath

        println "IIP:" + grailsApplication.config.cytomine.iipImageServer
    }

    def destroy = {
    }
}
