package be.cytomine.formats.tools.detectors

import loci.common.RandomAccessInputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import loci.common.ByteArrayHandle

class HuntTiff {

    private static final Logger LOGGER= Logger= LoggerFactory.getLogger(HuntTiff.class)

    private static final int LITTLEVERSION=0x49
    private static final int BIGVERSION=0x4d
    private static final int LITTLE_MAGICNUMBER=42
    private static final int BIG_MAGICNUMBER=43


    protected RandomAccessInputStream StreamIn

    private boolean bigTiff

    HuntTiff(RandomAccessInputStream StreamIn) {
        this.StreamIn = StreamIn
    }


    boolean TestHeader()
    {
        try
        {
            return checkHeader() != null
        }
        catch(IOException e)
        {
            return false
        }
    }

    Boolean checkHeader() throws IOException
    {
        if(StreamIn.length()<4)
            return null;
        // check the 2th byte
        StreamIn.seek(0)
        int lect1=StreamIn.read()
        int lect2=StreamIn.read()

        boolean Test1ittle=(lect1==LITTLEVERSION && lect2== LITTLEVERSION)
        boolean Testbig=(lect1==BIGVERSION && lect2==BIGVERSION)

        if(!Test1ittle && !Testbig)
            return null

        //check Number Magic

        StreamIn.order(Test1ittle)
        short magicNumber= StreamIn.readShort()
        bigTiff= (magicNumber==LITTLE_MAGICNUMBER)
        if(magicNumber!= BIG_MAGICNUMBER)
        {
            return null
        }

        return new Boolean(Test1ittle)
    }
}
