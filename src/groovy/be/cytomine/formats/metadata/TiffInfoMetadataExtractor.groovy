package be.cytomine.formats.metadata

import be.cytomine.formats.CytomineFile


class TiffInfoMetadataExtractor extends MetadataExtractor {

    private CytomineFile file

    public TiffInfoMetadataExtractor(def file) {
        this.file = file
    }

    // https://www.awaresystems.be/imaging/tiff/tifftags/baseline.html
    def properties() {
        def infos = this.file.getTiffInfoOutput().tokenize('\n')
        def properties = [:]

        // Width and height
        int maxWidth = 0
        int maxHeight = 0
        infos.findAll { it.contains('Image Width:') }.each {
            def tokens = it.tokenize(" ")
            int width = Integer.parseInt(tokens.get(2))
            int height = Integer.parseInt(tokens.get(5))
            maxWidth = Math.max(maxWidth, width)
            maxHeight = Math.max(maxHeight, height)
        }
        properties << ["cytomine.width": maxWidth]
        properties << ["cytomine.height": maxHeight]

        // Resolution
        def resolutions = infos.findAll { it.contains('Resolution:') }.unique()
        if (resolutions.size() == 1) {
            def tokens = resolutions[0].tokenize(" ,/")
            properties << ["cytomine.physicalSizeX": Double.parseDouble(tokens.get(1).replaceAll(",", "."))]
            properties << ["cytomine.physicalSizeY": Double.parseDouble(tokens.get(3).replaceAll(",", "."))]
            if (tokens.size() >= 5 && !tokens.get(3).contains("unitless")) {
                def unit = tokens.get(4)
                properties << ["cytomine.physicalSizeXUnit": unit]
                properties << ["cytomine.physicalSizeYUnit": unit]
            }
        }

        // Bit/Sample
        def bps = infos.findAll { it.contains('Bit/Sample:')}.unique()
        if (bps.size() == 1) {
            def tokens = bps[0].tokenize(" ")
            properties << ["cytomine.bitPerSample": Integer.parseInt(tokens.get(1))]
        }

        // Sample/pixel
        def spp = infos.findAll { it.contains('Sample/Pixel:')}.unique()
        if (spp.size() == 1) {
            def tokens = spp[0].tokenize(" ")
            properties << ["cytomine.samplePerPixel": Integer.parseInt(tokens.get(1))]
        }

        // Colorspace
        def colorspaces = infos.findAll { it.contains('Photometric Interpretation:') }.unique()
        if (colorspaces.size() == 1) {
            def tokens = colorspaces[0].tokenize(":")
            def value = tokens.get(1).trim().toLowerCase()
            String colorspace
            if (value == "min-is-black" || value == "grayscale")
                colorspace = "grayscale"
            else if (value.contains("rgb"))
                colorspace = "rgb"
            else
                colorspace = value
            properties << ["cytomine.colorspace": colorspace]
        }

        return properties
    }
}
