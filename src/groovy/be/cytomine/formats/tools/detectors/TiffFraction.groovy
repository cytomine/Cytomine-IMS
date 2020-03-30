//package be.cytomine.formats.tools.detectors
//
//class TiffFraction extends Number implements Comparable<TiffFraction> {
//
//    protected long num, deno
//
//    TiffFraction(long num, long deno) {
//        this.num = num
//        this.deno = deno
//    }
//
//    int compareTo(TiffFraction o) {
//        long diff = (numer * q.denom - q.numer * denom);
//        if (diff > Integer.MAX_VALUE) diff = Integer.MAX_VALUE;
//        else if (diff < Integer.MIN_VALUE) diff = Integer.MIN_VALUE;
//        return (int) diff;    }
//
//    int intValue() {
//        return (int) longValue();    }
//
//    long longValue() {
//        return denom == 0 ? Long.MAX_VALUE : (numer / denom);
//    }
//
//    float floatValue() {
//        return (float) doubleValue();
//    }
//
//    double doubleValue() {
//        return denom == 0 ? Double.MAX_VALUE : ((double) numer / (double) denom);
//    }
//}
