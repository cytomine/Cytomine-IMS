package be.cytomine.formats.microscopy

import be.cytomine.formats.ImageFormat
import grails.util.Holders
import loci.common.services.DependencyException
import loci.common.services.ServiceException
import loci.common.services.ServiceFactory
import loci.formats.CoreMetadata
import loci.formats.FormatException
import loci.formats.IFormatReader
import loci.formats.ImageReader
import loci.formats.ImageWriter
import loci.formats.MissingLibraryException
import loci.formats.services.OMEXMLService
import loci.formats.services.OMEXMLServiceImpl
import utils.OMEXMLConvertor

import java.awt.image.BufferedImage


/**
 * Created by stevben on 11/09/14.
 */
class OMEXML extends ImageFormat {

    public OMEXML() {
        extensions = ["tif", "tiff"]
        mimeType = "ome/tiff"
    }

    private static String SPLITING_MODE = '-z%z-t%t-c%c.ome.tiff'; //"_Z%z_T%t_C%c_S%s.ome.tiff" //OME SPITTING MODE

    private excludeDescription = [
            "Not a TIFF",
            "<iScan",
            "Hamamatsu",
            "Aperio",
            "Leica",
            "PHILIPS"
    ]

    public boolean detect() {
        String command = "file  $absoluteFilePath"
        def proc = command.execute()
        proc.waitFor()
        String stdout = proc.in.text
        boolean tiff = stdout.contains("TIFF image data")
        if (!tiff) return false

        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text

        boolean notTiff = false
        excludeDescription.each {
            notTiff |= tiffinfo.contains(it)
        }
        if (notTiff) return false

        return (tiffinfo.contains("OME-XML")) //Present in XML-Header
    }


    public def convert(String workDir) {
        String source = absoluteFilePath
        String uuid = UUID.randomUUID().toString()
        String parentDirectory = new File(absoluteFilePath).getParent()
        String target = [parentDirectory, uuid + SPLITING_MODE].join(File.separator)

        //call to convert
        OMEXMLConvertor converter = new OMEXMLConvertor(source, target)
        boolean success = converter.testConvert(new ImageWriter(), true)

        def imageConverted = []
        if (success) {
            int z = converter.getZ();
            int t = converter.getT();
            int c = converter.getC();
            println "z = $z, t = $t, c = $c"
            for (int _z = 0; _z < z; _z++) {
                for (int _t = 0; _t < t; _t++) {
                    for (int _c = 0; _c < c; _c++) {
                        String nestedFilename = [parentDirectory, uuid + "-z" + _z + "-t" + _t + "-c" + _c + ".ome.tiff"].join(File.separator)
                        println nestedFilename
                        assert(new File(nestedFilename).exists())
                        imageConverted << [file : nestedFilename, z : _z, t : _t, c : _c]
                    }
                }
            }
            return imageConverted
        }

        return null //to do : throw error instead of return null
    }

    public def properties() {

        OMEXMLConvertor convertor = new OMEXMLConvertor(absoluteFilePath, File.createTempFile("tmp", SPLITING_MODE).absolutePath)
        convertor.testConvert(new ImageWriter(), true)

        def properties = []

        properties << [ key : "mimeType", value : mimeType]
        properties << [ key : "cytomine.multidim.c", value : convertor.getC()]
        properties << [ key : "cytomine.multidim.z", value : convertor.getZ()]
        properties << [ key : "cytomine.multidim.t", value : convertor.getT()]
        properties << [ key : "cytomine.width", value : convertor.getWidth()]
        properties << [ key : "cytomine.height", value : convertor.getHeight()]
        properties << [ key : "cytomine.resolution", value : null ]
        properties << [ key : "cytomine.magnification", value : null ]

        return properties
    }

    private ImageReader getOMEXMLReader() {
        IFormatReader reader = new ImageReader()
        reader.setGroupFiles(true); //group files
        reader.setMetadataFiltered(true)
        reader.setOriginalMetadataPopulated(true)
        OMEXMLService service = null
        try {
            ServiceFactory factory = new ServiceFactory()
            service = factory.getInstance(OMEXMLService.class)
            reader.setMetadataStore(service.createOMEXMLMetadata())
        }
        catch (DependencyException de) {
            throw new MissingLibraryException(OMEXMLServiceImpl.NO_OME_XML_MSG, de)
        }
        catch (ServiceException se) {
            throw new FormatException(se)
        }
        //reader is an object instance of an Interface "IFormatReader" for all biological file format readers.
        //bind the reader to OMEXMLMetadata object
        reader.setId(absoluteFilePath)

        return reader
    }

    public BufferedImage thumb(int maxSize) {

    }

    public BufferedImage associated(String label) {
        if (label == "macro" || label == "preview") {
            thumb(256)
        } else if (label == "preview") {
            thumb(1024)
        }
    }
}
