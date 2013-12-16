class BootStrap {

    def imageServerService

    def init = { servletContext ->
        imageServerService.start()
    }
    def destroy = {
    }
}
