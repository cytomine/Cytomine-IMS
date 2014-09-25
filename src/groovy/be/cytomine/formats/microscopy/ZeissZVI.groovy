package be.cytomine.formats.microscopy

import be.cytomine.formats.ImageFormat
import grails.util.Holders
import loci.common.services.ServiceFactory
import loci.formats.ImageReader
import loci.formats.meta.IMetadata
import loci.formats.out.OMETiffWriter
import loci.formats.services.OMEXMLService
import utils.FilesUtils
import utils.ProcUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 11/09/14.
 */
class ZeissZVI extends ImageFormat {

    public ZeissZVI() {
        extensions = ["zvi"]
        mimeType = "zeiss/zvi"
    }

    //weak detection
    public boolean detect() {
        String extension = FilesUtils.getExtensionFromFilename(absoluteFilePath)
        return extensions.contains(extension)
    }

    //convert from supported bioformat tool to OME-XML
    public def convert(String workingPath) {

        String source = absoluteFilePath
        String target = [new File(absoluteFilePath).getParent(), UUID.randomUUID().toString() + "-ome-xml.tif"].join(File.separator)

        ImageReader reader = new ImageReader()
        OMETiffWriter writer = new OMETiffWriter()

        // record metadata to OME-XML format
        //Creation of OMEXMLMetadata object
        ServiceFactory factory = new ServiceFactory()
        OMEXMLService service = factory.getInstance(OMEXMLService.class)
        IMetadata omexmlMeta = service.createOMEXMLMetadata()

        //attaching the OMEXMLMetadata object to the reader
        reader.setMetadataStore(omexmlMeta)
        reader.setId(source)

        // configure OME-TIFF writer
        // The OMEXMLMetadata object is then fed to the OMETiffWriter, which extracts the appropriate OME-XML string
        // and embeds it into the OME-TIFF file properly
        writer.setMetadataRetrieve(omexmlMeta)
        writer.setId(target)

        // write target image planes.
        int seriesCount = reader.getSeriesCount()

        for (int s=0; s<seriesCount; s++) {
            reader.setSeries(s)
            writer.setSeries(s)
            int planeCount = reader.getImageCount()
            for (int p=0; p<planeCount; p++) {
                byte[] plane = reader.openBytes(p)
                // write planes to separate files and
                writer.saveBytes(p, plane)
                System.out.print(".")
            }
        }

        writer.close()
        reader.close()

        target
    }

    public BufferedImage thumb(int maxSize) {
        return null
    }

    public BufferedImage associated(String label) {
        if (label == "macro" || label == "preview") {
            thumb(256)
        } else if (label == "preview") {
            thumb(1024)
        }
    }
}
