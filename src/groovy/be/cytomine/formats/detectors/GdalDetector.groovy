package be.cytomine.formats.detectors

import grails.converters.JSON

trait GdalDetector extends Detector {
    boolean detect() {
        def output = JSON.parse(this.file.getGdalInfoOutput())
        return !output?.coordinateSystem?.wkt?.isEmpty()
    }
}