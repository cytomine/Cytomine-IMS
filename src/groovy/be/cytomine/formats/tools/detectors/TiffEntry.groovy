//package be.cytomine.formats.tools.detectors
//
//class TiffEntry implements Comparable<Object>{
//
//    private long valueOffset;
//    private int tag;
//    private int valueCount;
//    private TiffType type;
//
//    TiffEntry(int tag, TiffType type, int valueCount, long valueOffset) {
//        this.tag = tag;
//        this.type = type;
//        this.valueCount = valueCount;
//        this.valueOffset = valueOffset;
//    }
//
//
//    int compareTo(Object o) {
//        if (!(o instanceof TiffEntry)) return 1;
//        long offset = ((TiffEntry) o).getValueOffset();
//
//        if (offset == getValueOffset()) return 0;
//        return offset < getValueOffset() ? 1 : -1;
//        return 0
//    }
//
//    int getTag() { return tag; }
//    TiffType getType() { return type; }
//    int getValueCount() { return valueCount; }
//    long getValueOffset() { return valueOffset; }
//
//
//     String toString()
//     {
//        return "tag = " + tag + ", type = " + type + ", count = " + valueCount +
//                ", offset = " + valueOffset;
//    }
//}
