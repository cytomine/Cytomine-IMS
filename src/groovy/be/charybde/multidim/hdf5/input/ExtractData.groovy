package be.charybde.multidim.hdf5.input;

import ch.systemsx.cisd.base.mdarray.MDShortArray;

/**
 * Created by laurent on 20.12.16.
 */
public abstract class ExtractData {

    public abstract MDShortArray extract2DTile(int startX, int startY, int dim, int wid, int hei, MDShortArray base)
    public abstract MDShortArray extract2DTile(int startX, int startY, int wid, int hei, int depth)
    public abstract int getImageWidth();
    public abstract int getImageHeight();
    public abstract int getImageDepth();
    public abstract void getImage(int i)

}
