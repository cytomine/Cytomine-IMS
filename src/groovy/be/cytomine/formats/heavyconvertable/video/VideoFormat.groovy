package be.cytomine.formats.heavyconvertable.video

import be.cytomine.exception.ConversionException
import be.cytomine.formats.NotNativeFormat
import be.cytomine.formats.tools.CytomineFile
import be.cytomine.formats.tools.metadata.FfProbeMetadataExtractor
import grails.util.Holders
import utils.ProcUtils

abstract class VideoFormat extends NotNativeFormat {

    String FFPROBE_FORMAT_IDENTIFIER = null

    def convert() {
        CytomineFile target = new CytomineFile(this.file.parent, "conversion")
        ProcUtils.executeOnShell("mkdir -p ${target.absolutePath}")
        ProcUtils.executeOnShell("chmod -R 777 ${target.absolutePath}")

        def name = this.file.name - ".${this.file.extension()}"
        def options = "-hide_banner -an -sn -y"
        def executable = Holders.config.cytomine.ims.conversion.ffmpeg.executable
        def command = """$executable -i ${this.file.absolutePath} $options ${target.absolutePath}/${name}_T%d.jpg """
        if (ProcUtils.executeOnShell(command).exit != 0 || !target.exists())
            throw new ConversionException("${file.absolutePath} hasn't been converted to ${target.absolutePath}")

        return target.listFiles().collect {
            def t = it.name - name - "_T" - ".jpg"
            return new CytomineFile(it.absolutePath as String, 0, 0, t)
        }
    }

    def properties() {
        return new FfProbeMetadataExtractor(this.file).properties()
    }

    def cytomineProperties() {
        def properties = super.cytomineProperties()

        properties << ["cytomine.bitPerSample": 8]
        properties << ["cytomine.samplePerPixel": 3]
        properties << ["cytomine.colorspace": "rgb"]

        return properties
    }
}
