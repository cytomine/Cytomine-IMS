package be.cytomine.formats

import grails.util.Holders
import utils.ProcUtils
import utils.ServerUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 10/05/14.
 */
abstract class ImageFormat extends Format {

    def grailsApplication
    public String[] extensions = null
    public String mimeType = null
    public String widthProperty = "width"
    public String heightProperty = "height"
    public String resolutionProperty = "resolution"
    public String magnificiationProperty = "magnificiation"
    public List<String> iipURL = ServerUtils.getServers(Holders.config.cytomine.iipImageServerCyto)

    abstract public def convert(String workingPath)
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

    String cropURL(def params, def charset = "UTF-8") {
        String fif = URLEncoder.encode(params.fif,charset)
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

		int maxWidthOrHeight = Holders.config.cytomine.maxCropSize
		if (width > maxWidthOrHeight || height > maxWidthOrHeight) {		
			int tmpWidth = width
			int tmpHeight = height
			int zoom = 0		
			while (tmpWidth > maxWidthOrHeight || tmpHeight > maxWidthOrHeight) {
				tmpWidth = tmpWidth / 2
				tmpHeight = tmpHeight / 2				
				zoom++
			}
			/*
			Ruven P. (author of IIP Image) 
			In fact, the region is calculated from the WID or HEI given, not from
			the full image size. So you get the requested region on the virtual
			750px resize. I guess you were expecting to get a region exactly of size
			WID?

			This is something that seems to have caused confusion with others also
			and perhaps the way it works in counter intuitive, so I'm considering
			changing the behaviour in the 1.0 release and have WID or HEI define the
			final region size rather than the virtual image size. In the meantime,
			the way to get around it is to calculate the appropriate WID that the
			full image would be. So if your image is x pixels wide, give WID the
			value of x/2 to get a 750px wide image. 
			*/
			int hei = imageHeight / (height / tmpHeight)
			return "${ServerUtils.getServer(iipURL)}?FIF=$fif&RGN=$x,$y,$w,$h&HEI=$hei&CVT=jpeg"
		} else {
			return "${ServerUtils.getServer(iipURL)}?FIF=$fif&RGN=$x,$y,$w,$h&CVT=jpeg"
		}
    }

    public String tileURL(fif, params) {
        //return "$iipURL?zoomify=$params.zoomify"
        return "${ServerUtils.getServer(iipURL)}?zoomify=$fif/TileGroup$params.tileGroup/$params.z-$params.x-$params.y" + ".jpg&mimeType=$params.mimeType"
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
