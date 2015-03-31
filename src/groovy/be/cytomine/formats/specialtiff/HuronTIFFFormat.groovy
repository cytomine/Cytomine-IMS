package be.cytomine.formats.specialtiff

/**
 * Created by hoyoux on 31.03.15.
 */
class HuronTIFFFormat extends TIFFToConvert {
    public HuronTIFFFormat () {
        extensions = ["tif", "tiff"]
    }

    public boolean detect() {
        String tiffinfo = getTiffInfo()

        return !tiffinfo.contains("Compression Scheme: JPEG") && !tiffinfo.contains("Photometric Interpretation: YCbCr") &&
                tiffinfo.contains("Compression Scheme: None") && tiffinfo.contains("Photometric Interpretation: RGB color") &&
                tiffinfo.contains("Source = Bright Field")
    }
}
