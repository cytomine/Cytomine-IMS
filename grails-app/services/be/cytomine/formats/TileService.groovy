package be.cytomine.formats

class TileService {

    def getTileUrl(def params) {
        String fif = params.zoomify
        /*remove the "/" at the end of the path injected by openlayers (OL2).
          I Did not find the way to avoid it from OL2 (BS)
         */
        if (fif.endsWith("/"))
            fif = fif.substring(0, fif.length()-1)
        String mimeType = params.mimeType
        ImageFormat imageFormat = FormatIdentifier.getImageFormatByMimeType(fif, mimeType)
        //todo, use mimetype to have imageFormat identification
        return imageFormat.tileURL(fif, params)
    }
}
