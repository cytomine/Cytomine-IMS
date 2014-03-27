package be.cytomine

class ProxyController {

    /**
     * Get Image Tile
     * @param id
     * @param params
     */
    def index() {
        withFormat {
            jpeg {
                byte[] data = new URL(params.url).getBytes()
                response.contentType = "image/jpeg"
                response.contentLength = data.length
                response.getOutputStream() << data
            }
        }
    }

}
