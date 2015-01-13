import grails.util.Holders

class BootStrap {

    def grailsApplication

    def init = { servletContext ->
        println "Config file: "+ new File("imageserverconfig.properties").absolutePath

        println "iipImageServerBase:" + grailsApplication.config.cytomine.iipImageServerBase
        println "iipImageServerJpeg2000:" + grailsApplication.config.cytomine.iipImageServerJpeg2000
        println "iipImageServerVentana:" + grailsApplication.config.cytomine.iipImageServerVentana
        println "iipImageServerCyto:" + grailsApplication.config.cytomine.iipImageServerCyto

        Holders.config.cytomine.maxCropSize = Integer.parseInt(Holders.config.cytomine.maxCropSize+"")
    }

    def destroy = {
    }
}
