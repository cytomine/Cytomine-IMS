package be.cytomine.formats.detectors


trait OpenSlideDetector extends Detector {

//    abstract String vendor

    boolean detect() {
        return this.file.getOpenSlideVendor() == this.vendor
    }
}