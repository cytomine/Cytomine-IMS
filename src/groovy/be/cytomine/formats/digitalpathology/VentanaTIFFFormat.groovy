package be.cytomine.formats.digitalpathology

import be.cytomine.formats.standard.TIFFFormat
import grails.util.Holders
import org.openslide.OpenSlide
import utils.ProcUtils

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

/**
 * Created by stevben on 28/04/14.
 */
class VentanaTIFFFormat extends TIFFFormat {

    public VentanaTIFFFormat () {
        extensions = ["tif", "tiff"]
        mimeType = "ventana/tif"
        widthProperty = null //to compute
        heightProperty = null //to compute
        resolutionProperty = "ScanRes"
        magnificiationProperty = "Magnification"
    }

    private excludeDescription = [
            "Not a TIFF",
            "Make: Hamamatsu",
            "Leica",
            "ImageDescription: Aperio Image Library",
            "PHILIPS"
    ]

    public boolean detect() {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text

        boolean notTiff = false
        excludeDescription.each {
            notTiff |= tiffinfo.contains(it)
        }
        if (notTiff) return false

        return (tiffinfo.contains("<iScan")) //ventana signature


    }

    String convert(String workingPath) {
        boolean convertSuccessfull = true

        String source = absoluteFilePath
        String target = [new File(absoluteFilePath).getParent(), UUID.randomUUID().toString() + ".tif"].join(File.separator)

        def vipsExecutable = Holders.config.cytomine.vips
        def command = """$vipsExecutable im_vips2tiff $source:2 $target:jpeg:95,tile:256x256,pyramid,,,,8"""
        convertSuccessfull &= ProcUtils.executeOnShell(command) == 0

        if (convertSuccessfull) {
            return target
        }
    }



    public BufferedImage associated(String label) { //should be abstract
        if (label == "macro") {
            BufferedImage associatedImage = getTIFFSubImage(0)
            associatedImage = rotate90ToRight(associatedImage)
            return associatedImage
        }
    }

    public def properties() {
        File slideFile = new File(absoluteFilePath)
        def properties = [[key : "mimeType", value : mimeType]]
        if (slideFile.canRead()) {
            def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
            String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text

            //description
            def iScanStart = tiffinfo.indexOf('<iScan')
            def iScanEnd = tiffinfo.indexOf('</iScan>')
            def iScanXML = tiffinfo.substring(iScanStart, iScanEnd + '</iScan>'.length())
            def iScan = new XmlParser().parseText(iScanXML)
            iScan.attributes().each {
                properties << [key : it.key, value : it.value]
            }

            //metadata
            def metadataStart = tiffinfo.indexOf('<Metadata>')
            def metadataEnd = tiffinfo.indexOf('</Metadata>')
            def metadataXML = tiffinfo.substring(metadataStart, metadataEnd + '</Metadata>'.length())
            def metadata = new XmlParser().parseText(metadataXML)
            def prescanDataProperties = [
                    'SlideIdentifier',
                    'SizeImage',
                    'PixelsPerUnit',
                    'Unit',
                    'PixelMode'
            ]
            prescanDataProperties.each { key ->
                properties << [key : "prescanData.$key", value : metadata.PrescanData.AOI."@$key"]
            }
            def aoiIndex = [
                    'Identifier',
                    'Valid',
                    'SizeImage',
                    'Rectangle',
                    'PixelMode',
                    'Unit',
                    'PixelsPerUnit'
            ]
            aoiIndex.each { key ->
                properties << [key : "aoiIndex.$key", value : metadata.PrescanData.AOI."@$key"[0]]
            }
            def aoiParameters = [
                    'XEnd',
                    'YStart',
                    'CornerGrayValue',
                    'BottomLeftPt',
                    'BottomRightPt' ,
                    'FolderPath',
                    'AOIApproach',
                    'Sensitivity',
                    'LowProbMin',
                    'LowProbMax',
                    'SaturationQuantileMin',
                    'SaturationQuantileMax',
                    'AOIRectExtendDim',
                    'MergeDistance',
                    'FocusApproach',
                    'CropX'
            ]
            aoiParameters.each { key ->
                properties << [key : "aoiParameters.$key", value : metadata.AOIParameters."@$key"[0]]
            }

            if (resolutionProperty)
                properties << [ key : "cytomine.resolution", value : properties.find { it.key == resolutionProperty}?.value ]
            if (resolutionProperty)
                properties << [ key : "cytomine.magnification", value : properties.find { it.key == magnificiationProperty}?.value ]

            //get width & height from tiffinfo...
            int maxWidth = 0
            int maxHeight = 0
            tiffinfo.tokenize( '\n' ).findAll {
                it.contains 'Image Width:'
            }.each {
                def tokens = it.tokenize(" ")
                int width = Integer.parseInt(tokens.get(2))
                int height = Integer.parseInt(tokens.get(5))
                maxWidth = Math.max(maxWidth, width)
                maxHeight = Math.max(maxHeight, height)
            }
            properties << [ key : "cytomine.width", value : maxWidth ]
            properties << [ key : "cytomine.height", value : maxHeight ]
        }
        return properties
    }

    BufferedImage thumb(int maxSize) {
        def tiffinfoExecutable = Holders.config.cytomine.tiffinfo
        String tiffinfo = "$tiffinfoExecutable $absoluteFilePath".execute().text
        int numberOfTIFFDirectories = tiffinfo.count("TIFF Directory")
        getTIFFSubImage(numberOfTIFFDirectories - 2 - 1) /* - 2 because we have macro & roi as two first directories.
                                                        -1 because we want the smallest one */
        //:to do - scale the image to maxSize
    }



}
