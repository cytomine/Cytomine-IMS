package be.cytomine.formats.tools.detectors

import ij.plugin.Straightener
import loci.common.DataTools
import loci.common.RandomAccessInputStream

class OnDemandLongArray {

    private transient RandomAccessInputStream StreamIn
    private int size
    private long start

    OnDemandLongArray(RandomAccessInputStream RaiS) throws IOException
    {
        StreamIn= RaiS
        start = stream.getFilePointer()
    }

    long getValue(int index) throws  IOException
    {
        long fp = stream.getFilePointer()
        stream.seek(start + index *8)
        long value = StreamIn.readLong()
        StreamIn.seek(fp)
        return  value
    }

    long[] toArray() throws IOException
    {
        long fp = StreamIn.getFilePointer();
        StreamIn.seek(start);
        byte[] rawBytes = new byte[size * 8];
        StreamIn.readFully(rawBytes);
        StreamIn.seek(fp);

        return (long[]) DataTools.makeDataArray(rawBytes,8,false,StreamIn.isLittleEndian())
    }

    void close() throws IOException
    {
        if(StreamIn!= null)
        {
            StreamIn.close()
        }

        StreamIn=null
        size=0
        start=0
    }




}
