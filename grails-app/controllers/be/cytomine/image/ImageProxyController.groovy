package be.cytomine.image

class ImageProxyController extends ImageUtilsController {

    /**
     * Image Proxy method
     * @param url to fetch and return as image
     */
    def index() {
        responseImageFromUrl(params.url)
    }

}
