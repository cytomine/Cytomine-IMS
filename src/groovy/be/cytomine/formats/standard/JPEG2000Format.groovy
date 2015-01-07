package be.cytomine.formats.standard

import grails.util.Holders
import utils.FilesUtils
import utils.ServerUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 22/04/14.
 */
class JPEG2000Format extends CommonFormat {

    public JPEG2000Format() {
        extensions = ["jp2"]
        mimeType = "image/jp2"
        println "Holders.config.cytomine.iipJ2KImageServer=${Holders.config.cytomine.iipJ2KImageServer}"
        iipURL = ServerUtils.getServers(Holders.config.cytomine.iipJ2KImageServer)
    }

    public boolean detect() {
        //I check the extension for the moment because did not find an another way
        return FilesUtils.getExtensionFromFilename(absoluteFilePath).toLowerCase() == "jp2"
    }

    public def convert(String workingPath) {
        return null //nothing to do
    }


    public BufferedImage thumb(int maxSize) {
        //construct IIP2K URL
        //maxSize currently ignored because we need to know width of the image with IIP
        String thumbURL = "${ServerUtils.getServer(iipURL)}?fif=$absoluteFilePath&SDS=0,90&CNT=1.0&HEI=$maxSize&WID=$maxSize&CVT=jpeg&QLT=99"
        return ImageIO.read(new URL(thumbURL))
    }

    public def properties() {
        String propertiesURL = "${ServerUtils.getServer(iipURL)}?fif=$absoluteFilePath&obj=IIP,1.0&obj=Max-size&obj=Tile-size&obj=Resolution-number"
        String propertiesTextResponse = new URL(propertiesURL).text
        Integer width = null
        Integer height = null
        propertiesTextResponse.eachLine { line ->
            if (line.isEmpty()) return;

            def args = line.split(":")
            if (args.length != 2) return

            if (args[0].equals('Max-size')) {
                def sizes = args[1].split(' ')
                width = Integer.parseInt(sizes[0])
                height = Integer.parseInt(sizes[1])
            }
        }
        assert(width)
        assert(height)
        def properties = [[key : "mimeType", value : mimeType]]
        properties << [ key : "cytomine.width", value : width ]
        properties << [ key : "cytomine.height", value : height ]
        properties << [ key : "cytomine.resolution", value : null ]
        properties << [ key : "cytomine.magnification", value : null ]
        return properties
    }


}
