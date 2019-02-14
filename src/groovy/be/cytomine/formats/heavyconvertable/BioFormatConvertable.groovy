package be.cytomine.formats.heavyconvertable

import be.cytomine.exception.MiddlewareException
import be.cytomine.formats.Format
import grails.converters.JSON
import grails.util.Holders

/**
 * Created by hoyoux on 25.09.15.
 */
abstract class BioFormatConvertable extends Format implements IHeavyConvertableImageFormat {
    @Override
    String[] convert() {
        if(!Boolean.parseBoolean(Holders.config.bioformat.application.enabled)) throw new MiddlewareException("Convertor BioFormat not enabled");

        println "BIOFORMAT called !"
        def files = [];
        String error;

        String hostName = Holders.config.bioformat.application.location
        int portNumber = Integer.parseInt(Holders.config.bioformat.application.port);

        try {
            Socket echoSocket = new Socket(hostName, portNumber);
            PrintWriter out =
                    new PrintWriter(echoSocket.getOutputStream(), true);
            BufferedReader inp =
                    new BufferedReader(
                            new InputStreamReader(echoSocket.getInputStream()));

            out.println('{path:"'+absoluteFilePath+'",group:'+this.group+',onlyBiggestSerie:'+this.onlyBiggestSerie+'}');
            String result = inp.readLine();
            def json  = JSON.parse(result);
            files = json.files
            error = json.error;
        } catch (UnknownHostException e) {
            System.err.println(e.toString());
        }

        println "bioformat returns"
        println files.size()
        println files

        if(files ==[] || files == null) {
            if (error != null) {
                throw new MiddlewareException("BioFormat Exception : \n"+error);
            }
        }
        return files
    }

    abstract boolean getGroup();

    abstract boolean getOnlyBiggestSerie();
}
