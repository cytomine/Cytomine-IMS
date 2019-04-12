package be.cytomine.formats.heavyconvertable

import be.cytomine.exception.MiddlewareException
import be.cytomine.formats.CytomineFile
import be.cytomine.formats.NotNativeFormat
import grails.converters.JSON
import grails.util.Holders
import groovy.json.JsonOutput

/**
 * Created by hoyoux on 25.09.15.
 */
abstract class BioFormatConvertable extends NotNativeFormat /* implements IHeavyConvertableImageFormat */ {
    @Override
    def convert() {
        if (!Boolean.parseBoolean(Holders.config.bioformat.application.enabled))
            throw new MiddlewareException("Convertor BioFormat not enabled")

        println "BIOFORMAT called !"
        def files = []
        String error

        String hostName = Holders.config.bioformat.application.location
        int portNumber = Integer.parseInt(Holders.config.bioformat.application.port)

        println "hostname $hostName"
        println "port $portNumber"

        try {
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
            System.err.println(e.toString())
        }

        println "bioformat returns"
        println files

        if (files == [] || files == null) {
            if (error != null) {
                throw new MiddlewareException("BioFormat Exception : \n" + error)
            }
        }
        return files.collect { new CytomineFile(it.path as String, it.c, it.z, it.t) }
    }

    abstract boolean getGroup();

    abstract boolean getOnlyBiggestSerie();
}
