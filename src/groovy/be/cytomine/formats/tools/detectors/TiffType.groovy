package be.cytomine.formats.tools.detectors

import loci.common.enumeration.EnumException

enum TiffType {

    // IFD types
    BYTE(1, 1),
    ASCII(2, 1),
    SHORT(3, 2),
    LONG(4, 4),
    RATIONAL(5, 8),
    SBYTE(6, 1),
    UNDEFINED(7, 1),
    SSHORT(8, 2),
    SLONG(9, 4),
    SRATIONAL(10, 8),
    FLOAT(11, 4),
    DOUBLE(12, 8),
    IFD(13, 4),
    LONG8(16, 8),
    SLONG8(17, 8),
    IFD8(18, 8);

    private int code
    private int bytesPerElement

    private static final Map<Integer,TiffType> lookup =
            new HashMap<Integer,TiffType>()

    static {
        for(TiffType v : EnumSet.allOf(TiffType.class)) {
            lookup.put(v.getCode(), v);
        }
    }

    static TiffType get(int code) {
        TiffType toReturn = lookup.get(code);
        if (toReturn == null) {
            throw new EnumException("Unable to find IFDType with code: " + code);
        }
        return toReturn;
    }

    private TiffType(int code, int bytesPerElement) {
        this.code = code;
        this.bytesPerElement = bytesPerElement;
    }

    int getCode() {
        return code;
    }


    int getBytesPerElement() {
        return bytesPerElement;
    }
}