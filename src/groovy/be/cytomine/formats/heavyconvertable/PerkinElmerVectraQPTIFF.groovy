package be.cytomine.formats.heavyconvertable

import be.cytomine.formats.tools.detectors.HuntTiff
import be.cytomine.formats.tools.detectors.IFDTiff
import groovy.util.logging.Log4j
import loci.common.RandomAccessInputStream
import utils.MimeTypeUtils

@Log4j
class PerkinElmerVectraQPTIFF extends BioFormatConvertable{

    private static final String SOFTWARE_CHECK = "PerkinElmer-QPI";

    PerkinElmerVectraQPTIFF()
    {
        super()
        extensions=["qptiff"]
        mimeType= MimeTypeUtils.MIMETYPE_QPTIFF
    }



    @Override
    boolean detect()
    {
        boolean isType=false

        if(CheckExtension(file.getPath(),"qptiff"))
        {
            isType=true
        }
        else
        {
            try
            {
                RandomAccessInputStream stream= new RandomAccessInputStream(file.getPath())
                HuntTiff ConfigTiff= new HuntTiff(stream)
                if(!ConfigTiff.TestHeader())
                {
                    isType=false
                }
                else
                {
                    IFDTiff ifd= ConfigTiff.getIFDTiff()
                    if(ifd==null)
                    {
                        isType=false
                    }
                    else
                    {
                        ConfigTiff.fillIFDTiff(ifd)

                        String soft= ifd.getIFDTextValue(IFDTiff.SOFTWARE)
                        return soft !=null && soft.startsWith(SOFTWARE_CHECK)
                    }
                }

            }
            catch (IOException e)
            {
                LOGGER.debug("I/O exception during detect() evaluation.", e)
                return false
            }
        }

    }

    @Override
    boolean getGroup()
    {
        return true
    }

    @Override
    boolean getOnlyBiggestSerie() {
        return true
    }


}
