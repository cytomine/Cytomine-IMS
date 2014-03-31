package be.cytomine.formats

import loci.common.services.DependencyException
import loci.common.services.ServiceException
import loci.common.services.ServiceFactory
import loci.formats.*
import loci.formats.meta.MetadataStore
import loci.formats.services.OMEXMLService

//import loci.formats.in.DicomReader

class LociController {

    //test temporary
    def index() {
        // create a reader that will automatically handle any supported format
        IFormatReader reader = new ImageReader();
// tell the reader where to store the metadata from the dataset
        MetadataStore metadata;

        try {
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            metadata = service.createOMEXMLMetadata();
        }
        catch (DependencyException exc) {
            throw new FormatException("Could not create OME-XML store.", exc);
        }
        catch (ServiceException exc) {
            throw new FormatException("Could not create OME-XML store.", exc);
        }

        reader.setMetadataStore(metadata);
// initialize the dataset
        reader.setId("/Users/stevben/Downloads/Zeiss-4-Mosaic.zvi");
        println "metadata>>>>>>>>>>>>>>>>"
        reader.getMetadata().each {
            println it
        }
        println "end of metadata>>>>>>>>>>>>>>>>"

        // create a writer that will automatically handle any supported output format
        IFormatWriter writer = new ImageWriter();
// give the writer a MetadataRetrieve object, which encapsulates all of the
// dimension information for the dataset (among many other things)
        writer.setMetadataRetrieve(MetadataTools.asRetrieve(reader.getMetadataStore()));
// initialize the writer
        writer.setId("/Users/stevben/Desktop/Zeiss-1-Merged_conv.tiff");

        for (int series=0; series<reader.getSeriesCount(); series++) {
            reader.setSeries(series);
            writer.setSeries(series);

            for (int image=0; image<reader.getImageCount(); image++) {
                writer.saveBytes(image, reader.openBytes(image));
            }
        }
        reader.close();
        writer.close();
    }
}
