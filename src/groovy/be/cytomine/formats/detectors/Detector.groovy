package be.cytomine.formats.detectors

trait Detector {
//    abstract public CytomineFile file
    abstract boolean detect()
}