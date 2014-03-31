package be.cytomine.image

import be.cytomine.ImageController

class ImageProxyController extends ImageController {

    /**
     * Get Image Tile
     * @param id
     * @param params
     */
    def index() {
        responseImageFromUrl(params.url)
    }

}
