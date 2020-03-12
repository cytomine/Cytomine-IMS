package be.cytomine.formats.tools.detectors

import org.slf4j.LoggerFactory
import org.slf4j.Logger


class IFDTiff extends HashMap<Integer, Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IFDTiff.class)

    static final int LITTLE_ENDIAN=0
    static final int BIG_TIFF=1

    //IFD TAG
    static final int STRIP_OFFSETS = 273
    static final int TILE_OFFSETS = 324
    static final int STRIP_BYTE_COUNTS = 279
    static final int COLOR_MAP = 320
    static final int SOFTWARE = 305
    //Constructor

    IFDTiff(){ super() }


    String getIFDTextValue(int tag)
    {
        String value = null
        Object o = getIFDValue(tag)

        if(o instanceof String[])
        {
            StringBuilder sb= new StringBuilder()
            String[] s =(String[]) o
            for(int i=0;i<s.length;i++)
            {
                sb.append(s[i])
                if(i<s.length-1)
                {
                    sb.append("\n")
                }
            }
            value=sb.toString()
        }
        else if(o instanceof short[])
        {
            final StringBuilder sb= new StringBuilder()
            for(short s :((short[])o))
            {
               if(!Character.isISOControl((char)s))
               {
                   sb.append((char)s)
               }
               else if(s!=0)
               {
                   sb.append("\n")
               }

            }
            value=sb.toString()

        }
        else if(o != null)
        {
            value= o.toString()
        }

        if(value !=null)
        {
            value=value.replace("\r\n", "\n")
            value = value.replace('\r', '\n')
        }

        return  value
    }


    Object getIFDValue(int tag)
    {
        get(new Integer(tag))
    }
}
