package be.charybde.multidim.hdf5.output

/**
 * Created by laurent on 08.01.17.
 */
trait HDF5Geometry {
    private Boolean extract
    def private data = []

    def  getValues(){
        if(extract)
            return data
        return null
    }

    public void setData(def data){
        this.data = data
        this.extract = true
    }


    public Boolean isDataPresent(){
        return this.extract
    }

    def abstract getDataFromCache(def array)
    def abstract getDim()
    public abstract String getCSV()
}