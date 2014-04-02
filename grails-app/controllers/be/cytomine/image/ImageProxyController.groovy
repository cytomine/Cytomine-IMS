package be.cytomine.image

import be.cytomine.ImageController

class ImageProxyController extends ImageController {

    /**
     * Image Proxy method
     * @param url to fetch and return as image
     */
    def index() {
        responseImageFromUrl(params.url)
    }

}
