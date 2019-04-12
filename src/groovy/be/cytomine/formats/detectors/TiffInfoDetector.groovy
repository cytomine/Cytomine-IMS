package be.cytomine.formats.detectors

trait TiffInfoDetector extends Detector {
    def requiredKeywords
    def forbiddenKeywords
    def possibleKeywords

    boolean detect() {
        requiredKeywords = this.hasProperty("requiredKeywords")?.getProperty(this) ?: []
        forbiddenKeywords = this.hasProperty("forbiddenKeywords")?.getProperty(this) ?: []
        possibleKeywords = this.hasProperty("possibleKeywords")?.getProperty(this) ?: []

        def output = this.file.getTiffInfoOutput()

        /**
         * required = [a, b, c]
         * forbidden = [x, y, z]
         * possible = [m, n]
         *
         * detected = (a && b && c) && (!x && !y && !z) && (m || n) = (a && b && c) && !(x || y || z) && (m || n)
         */
        return requiredKeywords.every{ output.contains(it as String) } &&
                !forbiddenKeywords.any { output.contains(it as String) } &&
                (possibleKeywords.isEmpty() ? true : possibleKeywords.any { output.contains(it as String) })
    }
}