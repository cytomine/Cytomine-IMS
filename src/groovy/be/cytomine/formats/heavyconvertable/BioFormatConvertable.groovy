package be.cytomine.formats.heavyconvertable

import be.cytomine.exception.ConversionException
import be.cytomine.exception.MiddlewareException
import be.cytomine.formats.CytomineFile
import be.cytomine.formats.NotNativeFormat
import grails.converters.JSON
import grails.util.Holders
import groovy.json.JsonOutput
import groovy.util.logging.Log4j

@Log4j
abstract class BioFormatConvertable extends NotNativeFormat /* implements IHeavyConvertableImageFormat */ {
    @Override
    def convert() {
        if (!(Holders.config.cytomine.ims.conversion.bioformats.enabled as Boolean))
            throw new MiddlewareException("Convertor BioFormat not enabled")

        def files
        String error

        String hostName = Holders.config.cytomine.ims.conversion.bioformats.hostname
        int portNumber = Holders.config.cytomine.ims.conversion.bioformats.port as Integer
        try {
            log.info("BioFormats called on $hostName:$portNumber")
            Socket echoSocket = new Socket(hostName, portNumber)
            PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true)
            BufferedReader inp = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()))

            def message = [
                    path            : this.file.absolutePath,
                    group           : this.group as String,
                    onlyBiggestSerie: this.onlyBiggestSerie as String
            ]

            out.println(JsonOutput.toJson(message))
            String result = inp.readLine()
            def json = JSON.parse(result)
            files = json.files
            error = json.error
        } catch (UnknownHostException e) {
            throw new MiddlewareException(e.getMessage())
        }

        log.info("BioFormats returned ${files?.size()} files")

        if ((files == [] || files == null) && error != null) {
            throw new ConversionException("BioFormats Exception : $error")
        }
        return files.collect { new CytomineFile(it.path as String, it.c, it.z, it.t) }
    }

    abstract boolean getGroup();

    abstract boolean getOnlyBiggestSerie();
}
