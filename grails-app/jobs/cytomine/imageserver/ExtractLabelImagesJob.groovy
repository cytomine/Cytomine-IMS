package cytomine.imageserver

import be.cytomine.client.Cytomine
import be.cytomine.client.collections.ImageInstanceCollection
import be.cytomine.client.models.AbstractImage
import be.cytomine.client.models.ImageInstance
import grails.util.Holders
import org.openslide.AssociatedImage
import org.openslide.OpenSlide

import javax.imageio.ImageIO
import java.awt.image.BufferedImageFilter


class ExtractLabelImagesJob {

    def grailsApplication
    def cytomineCoreService

    static triggers = {
        simple repeatInterval: 5000l // execute job once in 5 seconds
    }

    def execute() {
        return
        String cytomineUrl = "http://localhost:8080/"
        String pubKey = grailsApplication.config.grails.imageServerPublicKey
        println pubKe y
        String privKey = grailsApplication.config.grails.imageServerPrivateKey
        println privKey
        Cytomine cytomine = new Cytomine(cytomineUrl, pubKey, privKey, "./", false)
        String storage = "/Users/stevben/cytomine_storage/37/"
        ImageInstanceCollection imageInstanceCollection =  cytomine.getImageInstances(cytomine.getProjects().get(0).id)
        for(int i=0;i<imageInstanceCollection.size();i++) {
            ImageInstance imageInstance = imageInstanceCollection.get(i)

            if (imageInstance.get("mime") == "ndpi" || imageInstance.get("mime") == "svs" || imageInstance.get("mime") == "mrxs" || imageInstance.get("mime") == "vms" || imageInstance.get("mime") == "scn") {
                String fullPath = storage + imageInstance.get("path")
                File slideFile = new File(fullPath)
                if (slideFile.canRead()) {
                    println fullPath
                    OpenSlide openSlide = new OpenSlide(slideFile)
                    openSlide.getAssociatedImages().each {
                        println it.key
                        AssociatedImage associatedImage = it.value
                        String outFilename = imageInstance.get("originalFilename")
                        outFilename = outFilename.replace(" ", "_").replace("/", "_")
                        String fullOutFilename = "/Users/stevben/Desktop/"+outFilename+"_"+it.key+".png"
                        println fullOutFilename
                        ImageIO.write(associatedImage.toBufferedImage(), "png", new File(fullOutFilename))
                    }

                    /*println openSlide.getLevelWidth(0)
                    openSlide.getProperties().each {
                        println it
                    }*/
                }

            }
        }

        // execute job


    }
}
