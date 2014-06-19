package be.cytomine.image

<<<<<<< Updated upstream
=======
import be.cytomine.ImageUtilsController

>>>>>>> Stashed changes
class ImageProxyController extends ImageUtilsController {

    /**
     * Image Proxy method
     * @param url to fetch and return as image
     */
    def index() {
        responseImageFromUrl(params.url)
    }

}
