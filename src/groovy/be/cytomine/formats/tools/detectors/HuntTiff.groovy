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

    private static final int BYTES_PER_ENTRY = 12
    private static final int BIG_TIFF_BYTES_PER_ENTRY = 20


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

    IFDTiff getIFDTiff() throws IOException
    {
        long OffsetTiff=getOffsetTiff()
        IFDTiff ifd= getIFD(OffsetTiff)
    }

    long getOffsetTiff() throws IOException
    {
        if(bigTiff)
        {
            StreamIn.skipBytes(4)

        }
        return getNextOffset(0)

    }

    long getNextOffset(long previous) throws IOException
    {
        if(bigTiff)
        {
            return StreamIn.readLong()
        }

        long offset=(previous & ~0xffffffffL) | (StreamIn.readUnsignedInt())

        if(offset < previous && offset !=0 && StreamIn.length() > Integer.MAX_VALUE)
        {
            offset += 0x100000000L
        }

        return offset
    }

    IFDTiff getIFD(long offsettiff) throws IOException
    {
        if(offsettiff<0 || offsettiff>= StreamIn.length())
            return null
        IFDTiff ifdTiff= new IFDTiff()
        ifdTiff.put(new Integer(IFDTiff.LITTLE_ENDIAN), new Boolean(StreamIn.isLittleEndian()))
        ifdTiff.put(new Integer(IFDTiff.BIG_TIFF), new Boolean(bigTiff))

        LOGGER.trace("getIFD: Start to collect IFD at {}",offsettiff)
        StreamIn.seek(offsettiff)
        long numEntries= bigTiff ? StreamIn.readLong() : StreamIn.readUnsignedShort()

        if(numEntries==0 || numEntries==1)
        {
            return ifdTiff
        }
        int bytesPerEntry = bigTiff ? BIG_TIFF_BYTES_PER_ENTRY : BYTES_PER_ENTRY

        int baseOffset = bigTiff ? 8 : 2

        for(int i=0;i<numEntries;i++)
        {
            StreamIn.seek(offsettiff + baseOffset + bytesPerEntry * i)



        }

    }
}
