package be.cytomine.formats.tools.detectors

import loci.common.Constants
import loci.common.RandomAccessInputStream
import loci.common.enumeration.EnumException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import loci.common.ByteArrayHandle

import javax.management.StandardEmitterMBean

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

        return ifd
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

            TiffEntry entry=null
            try
            {
                entry= readTiffEntry()
            }
            catch(EnumException e)
            {
                LOGGER.debug("",e)
            }

            if(entry ==null)
            {
                break

            }
            int count = entry.getValueCount()
            int tag = entry.getTag()
            long pointer = entry.getValueOffset()
            int bpe= entry.getType().getBytesPerElement()

            if(count <0 || bpe <=0)
            {
                StreamIn.skipBytes(bytesPerEntry - 4 - (bigTiff ? 8: 4))
                continue;
            }
            Object value=null

            long inputLen= StreamIn.length()
            if(count* bpe + pointer> inputLen)
            {
                int oldCount=count
                count=(int) ((inputLen - pointer)/bpe)
                if(count<0)
                {
                    count= oldCount
                }
            }
            if(count<0 || count> StreamIn.length())
            {
                break
            }
            else
            {
                value =getIFDValue(entry)
            }

            if(value !=null && ifdTiff.containsKey(new Integer(tag)))
            {
                ifdTiff.put(new Integer(tag),value)
            }
        }

        long newOffset = offsettiff + baseOffset + bytesPerEntry * numEntries

        if(newOffset < StreamIn.length())
        {
            StreamIn.seek(StreamIn.length())
        }

        return ifdTiff

    }

    TiffEntry readTiffEntry() throws IOException
    {
        int entryTag= StreamIn.readUnsignedShort()
        TiffType entryType
        try
        {
            entryType= IFDTiff.get(StreamIn.readUnsignedShort())
        }
        catch(EnumException e)
        {
            LOGGER.error("Error reading IFD type at; {}", StreamIn.getFilePointer())
            throw e
        }

        int valueCount =bigTiff ? 8: 4
        if(valueCount<0)
        {
            throw new RuntimeException("count of" + valueCount + "unexpected")
        }

        int nValueByes= valueCount * entryType.getBytesPerElement()
        int threshhold= bigTiff ? 8: 4
        long offset = nValueByes> threshhold ?getNextOffset(0) : StreamIn.getFilePointer()

        return new TiffEntry(entryTag,entryType,valueCount,offset)

    }

    Object getIFDValue (TiffEntry entry) throws IOException
    {
        TiffType type =entry.getType()
        int count = entry.getValueCount()
        long offset=entry.getValueOffset()

        if(offset>= StreamIn.length())
        {
            return null
        }

        if(offset>= StreamIn.getFilePointer())
        {
            StreamIn.seek(offset)
        }

        if(type== TiffType.BYTE)
        {
            if (count == 1) return new Short(StreamIn.readByte());
            byte[] bytes = new byte[count];
            StreamIn.readFully(bytes);
            // bytes are unsigned, so use shorts
            short[] shorts = new short[count];
            for (int j=0; j<count; j++) shorts[j] = (short) (bytes[j] & 0xff);
            return shorts;

        }
        else if(type== TiffType.ASCII)
        {
            byte[] ascii= new byte[count]
            StreamIn.read(ascii)
            int nullCount=0
            for(int j=0; j<count;j++)
            {
                if(ascii[j]==0 || j == count - 1)
                {
                    nullCount++
                }
            }

            String[] strings= nullCount==1 ? null: new String[nullCount]
            String s= null
            int c=0, ndx =-1
            for(int j=0; j<coun;j++)
            {
                if(ascii[j]==0)
                {
                    s= new String(ascii,ndx+1,j-ndx-1, Constants.ENCODING)
                    ndx=j

                }
                else if(j== count-1)
                {
                    s= new String(ascii, ndx+1,j-ndx, Constants.ENCODING)
                }
                else
                {
                    s=null

                }
                if(strings != null && s!=null)
                {
                    strings[c++]=s

                }

            }
            return strings == null ? (Object) s: strings
        }
        else if(type== TiffType.SHORT)
        {
            if(count ==1)
            {
                return new Integer(StreamIn.readUnsignedShort())

            }
            int[] shorts= new int[count]
            for(int j=0;j<count;j++)
            {
                shorts[j]=StreamIn.readUnsignedShort()
            }
            return shorts
        }
        else if(type==TiffType.LONG || type == TiffType.IFD)
        {
            if(count==1)
            {
                return new Long(StreamIn.readUnsignedInt())
            }
            long[] longs= new long[count]
            for(int j=0; j<count; j++)
            {
                if(StreamIn.getFilePointer()+4<= StreamIn.length())
                {
                    longs[j]=StreamIn.readUnsignedInt()
                }
            }
            return longs
        }
        else if(type == TiffType.LONG8 || type == TiffType.SLONG8 || type== TiffType.IFD8)
        {
            if(count==1)
            {
                return new Long(StreamIn.readLong())

            }
            long[] longs =null

            if(entry.getTag()==IFDTiff.STRIP_OFFSETS || entry.getTag()== IFDTiff.TILE_OFFSETS || entry.getTag()==IFDTiff.STRIP_BYTE_COUNTS || entry.getTag()== IFDTiff.STRIP_BYTE_COUNTS)
            {
                OnDemandLongArray offsets= new OnDemandLongArray(StreamIn)
                offsets.size(count)
                return offsets

            }
            else
            {
                longs= new long[count]
                for(int j=0; j<count;j++)
                {
                    longs[j]= StreamIn.readLong()
                }
                return  longs
            }

        }
        else if(type == TiffType.RATIONAL || type == TiffType.SRATIONAL)
        {
            if(count==1)
            {
                return new TiffFraction(StreamIn.readUnsignedInt(),StreamIn.readUnsignedInt())
            }
            TiffFraction[] tabFracTiff= new TiffFraction[count]
            for(int j=0; j<count;j++)
            {
                tabFracTiff[j]= new TiffFraction(StreamIn.readUnsignedInt(), StreamIn.readUnsignedInt())
            }
            return tabFracTiff
        }
        else if(type == TiffType.SBYTE || type == TiffType.UNDEFINED)
        {
            if(count ==1)
            {
                return  new Byte(StreamIn.readByte())
            }
            byte[] bytes= new Byte[count]
            StreamIn.read(bytes)
            return bytes
        }
        else if(type == TiffType.SSHORT)
        {
            if(count==1)
            {
                return new Short(StreamIn.readShort())
            }
            short [] shorts = new short[count]
            for(int j=0; j<count; j++)
            {
                shorts[j]=StreamIn.readShort()
            }
            return shorts
        }
        else if(type== TiffType.SLONG)
        {
            if(count==1)
            {
                return new Integer(StreamIn.readInt())
            }
            int[] Slongs= new int[count]
            for(int j=0;j<count;j++)
            {
                Slongs[j]=StreamIn.readInt()
            }
            return Slongs
        }
        else if(type== TiffType.FLOAT)
        {
            if(count ==1)
            {
                return new Float(StreamIn.readFloat())
            }
            float[] Floats= new float[count]
            for(int j=0;j<count;j++)
            {
                Floats[j]= StreamIn.readFloat()
            }
            return Floats
        }
        else if(type== TiffType.DOUBLE)
        {
            if(count==1)
            {
                return new Double(StreamIn.readDouble())
            }
            double[] Doubles= new Double[count]

            for(int j=0;j<count;j++)
            {
                Doubles[j]= StreamIn.readDouble()
            }
            return Doubles
        }
        return null

    }

    void fillIFDTiff(IFDTiff ifd) throws IOException
    {
        HashSet<TiffEntry> entries= new HashSet<TiffEntry>()

        for(Object key: ifd.keySet())
        {
            if(ifd.get(key) instanceof TiffEntry)
            {
                entries.add((TiffEntry) ifd.get(key))
            }
        }

        for(TiffEntry entry : entries)
        {
            if((entry.getValueCount()<10*1024*1024 || entry.getTag()<32768) && entry.getTag() != IFDTiff.COLOR_MAP )
            {
                ifd.put(new Integer(entry.getTag()),getIFDValue(entry))
            }
        }
    }
}
