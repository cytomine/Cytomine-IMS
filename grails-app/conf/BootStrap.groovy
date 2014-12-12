import grails.util.Holders

class BootStrap {

    def grailsApplication

    def init = { servletContext ->
        println "Config file: "+ new File("imageserverconfig.properties").absolutePath

        println "IIP:" + grailsApplication.config.cytomine.iipImageServer

        Holders.config.cytomine.maxCropSize = Integer.parseInt(Holders.config.cytomine.maxCropSize+"")
    }

    def destroy = {
    }
}
