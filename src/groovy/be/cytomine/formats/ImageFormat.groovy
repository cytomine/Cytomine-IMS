package be.cytomine.formats

import grails.util.Holders
import utils.ProcUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 10/05/14.
 */
abstract class ImageFormat extends Format {

    protected final static String ARGS_PREFIX = "?"
    protected final static String ARGS_DELIMITER = "&"
    protected final static String ARGS_EQUAL = "="
    protected final static int    TILE_SIZE = 256

    public String[] extensions = null
    public String mimeType = null
    public String widthProperty = "width"
    public String heightProperty = "height"
    public String resolutionProperty = "resolution"
    public String magnificiationProperty = "magnificiation"

    abstract public String convert(String workingPath)
    abstract BufferedImage associated(String label)
    abstract BufferedImage thumb(int maxSize)

    public String[] associated() {
        return ["macro"]
    }

    public def properties() {
        BufferedImage bufferedImage = ImageIO.read(new File(absoluteFilePath))
        def properties = []
        properties << [ key : "cytomine.width", value : bufferedImage.getWidth() ]
        properties << [ key : "cytomine.height", value : bufferedImage.getHeight() ]
        properties << [ key : "cytomine.resolution", value : null ]
        properties << [ key : "cytomine.magnification", value : null ]
        return properties
    }



    //useless
    /*
    def iipmetadata() {
        LinkedList<String> args = new LinkedList<String>()
        args.add("FIF" + ARGS_EQUAL +  absoluteFilePath)
        args.add("obj" + ARGS_EQUAL +  "IIP,1.0")
        args.add("obj" + ARGS_EQUAL +  "Max-size")
        args.add("obj" + ARGS_EQUAL +  "Tile-size")
        args.add("obj" + ARGS_EQUAL +  "Resolution-number")
        def url = "http://localhost:8081/fcgi-bin/iipsrv.fcgi?" + args.join(ARGS_DELIMITER)
        def iipmetadata = new URL(url).text
        def metadata = [:]
        println iipmetadata
        iipmetadata.split("\n").each {
            def _metadata = it.split(":")
            def key = _metadata[0]
            def value = _metadata[1]
            if (key == 'Max-size') {
                metadata.width = value.split(' ')[0]
                metadata.height = value.split(' ')[1]
            }
            if (key == 'Tile-size') {
                metadata.tile_size_w = value.split(' ')[0]
                metadata.tile_size_h = value.split(' ')[1]
            }
            if (key == 'Resolution-number') {
                metadata.resolution_number = value
            }
        }
        metadata
    }  */


    protected BufferedImage rotate90ToRight( BufferedImage inputImage ){
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage returnImage = new BufferedImage( height, width , inputImage.getType()  );

        for( int x = 0; x < width; x++ ) {
            for( int y = 0; y < height; y++ ) {
                returnImage.setRGB( height - y - 1, x, inputImage.getRGB( x, y  )  );
            }
        }
        return returnImage;
    }

    protected BufferedImage getTIFFSubImage(int index) {
        boolean convertSuccessfull = true

        println ImageIO.getReaderFormatNames()
        String source = absoluteFilePath
        File target = File.createTempFile("label", ".jpg")
        String targetPath = target.absolutePath

        println "target=" + target.getPath()
        def vipsExecutable = Holders.config.grails.vips
        def command = """$vipsExecutable im_copy $source:$index $targetPath"""
        convertSuccessfull &= ProcUtils.executeOnShell(command) == 0

        BufferedImage labelImage = null
        if (convertSuccessfull) {
            println targetPath
            println new File(targetPath).exists()
            labelImage = ImageIO.read(target)
            //labelImage = rotate90ToRight(labelImage)
            assert(labelImage)
        }
        target.delete()
        return labelImage
    }
}
