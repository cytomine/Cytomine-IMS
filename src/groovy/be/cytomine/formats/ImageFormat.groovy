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
    public String iipURL = Holders.config.cytomine.iipImageServer

    abstract public String convert(String workingPath)
    abstract BufferedImage associated(String label)
    abstract BufferedImage thumb(int maxSize)

    public String[] associated() {
        return ["macro"]
    }

    public def properties() {
        BufferedImage bufferedImage = ImageIO.read(new File(absoluteFilePath))
        def properties = [[key : "mimeType", value : mimeType]]
        properties << [ key : "cytomine.width", value : bufferedImage.getWidth() ]
        properties << [ key : "cytomine.height", value : bufferedImage.getHeight() ]
        properties << [ key : "cytomine.resolution", value : null ]
        properties << [ key : "cytomine.magnification", value : null ]
        return properties
    }

    String cropURL(def params) {
        String fif = params.fif
        int topLeftX = params.int('topLeftX')
        int topLeftY = params.int('topLeftY')
        int width = params.int('width')
        int height = params.int('height')
        int imageWidth = params.int('imageWidth')
        int imageHeight = params.int('imageHeight')
        def x = (topLeftX == 0) ? 0 : 1/(imageWidth / topLeftX)
        def y = ((imageHeight - topLeftY) == 0) ? 0 : 1/(imageHeight / (imageHeight - topLeftY))
        def w = (width == 0) ? 0 : 1/(imageWidth / width)
        def h = (height == 0) ? 0 : 1/(imageHeight / height)

        /*if (params.int('scale')) {
            int scale = params.int('scale')
            if (height > scale) {
                int hei = Math.round(imageHeight / Math.ceil(height / scale))
                return "FIF=$fif&RGN=$x,$y,$w,$h&HEI=$hei&CVT=jpeg"
            } else if (width > scale) {
                int wid = Math.round(imageWidth / Math.ceil(width / scale))
                return "FIF=$fif&RGN=$x,$y,$w,$h&WID=$wid&CVT=jpeg"
            }
        } else {*/
            return "$iipURL?FIF=$fif&RGN=$x,$y,$w,$h&CVT=jpeg"
        //}
    }

    public String tileURL(fif, params) {
        return "$iipURL?zoomify=$fif/TileGroup$params.tileGroup/$params.z-$params.x-$params.y" + ".jpg"
    }

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
        def vipsExecutable = Holders.config.cytomine.vips
        def command = """$vipsExecutable im_copy $source:$index $targetPath"""
        println command
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
