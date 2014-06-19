package be.cytomine.formats

import be.cytomine.client.Cytomine
import be.cytomine.client.models.AbstractImage
import be.cytomine.ImageUtilsController
import grails.converters.JSON

import java.awt.image.BufferedImage

class OpenSlideController extends ImageUtilsController{

    def cytomineService
    def imageProcessingService
    def openSlideService

    /**
     * Return the labels of all the associated images (e.g. macro, label, ...) contained in a virtual slide
     * @return a list of labels
     */
    def associated() {
        Long id = params.long("id")

        //init cytomine instance connection
        Cytomine cytomine = cytomineService.getCytomine(params.cytomineUrl)

        //fetch abstractimage from cytomine instance in order to get the fullpath
        AbstractImage abstractImage = cytomine.getAbstractImage(id)
        String fullPath = abstractImage.getAt("fullPath")

        //get the labels associated using Openslide library
        def labels = openSlideService.getAssociatedImages(fullPath)

        render labels as JSON
    }

    /**
     * Return an associated image contained in a virtual slide (e.g. macro, labels, ...).
     * Rotate it if necessary (the widest side horizontally)
     */
    def associatedImage() {
        Long id = params.long("id")
        Integer maxWidth = params.int("maxWidth")
        String label = params.label

        //init cytomine instance connection in order to get the fullPath and the mime type
        Cytomine cytomine = cytomineService.getCytomine(params.cytomineUrl)
        AbstractImage abstractImage = cytomine.getAbstractImage(id)
        String fullPath = abstractImage.getAt("fullPath")
        String mime = abstractImage.getAt("mime")

        BufferedImage associatedImage = openSlideService.getAssociatedImage(fullPath, label)

        //rotate if necessary
        String[] mimeToRotate = ["scn", "mrxs"] //the mime formats which requires a rotation
        if (mimeToRotate.contains(mime)) {
            associatedImage = imageProcessingService.rotate90ToRight(associatedImage)
        }

        //resize it with is larger than the maxWidth argument (optional)
        if (maxWidth && associatedImage.width > maxWidth) {
            int w = maxWidth
            int h = associatedImage.height / (associatedImage.width / maxWidth)
            associatedImage = imageProcessingService.resizeImage(associatedImage, w, h)
        }

        responseBufferedImage(associatedImage)
    }
}
