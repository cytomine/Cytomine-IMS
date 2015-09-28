package be.cytomine.formats.heavyconvertable

import be.cytomine.formats.ConvertableImageFormat
import be.cytomine.formats.Format
import be.cytomine.formats.IConvertableImageFormat
import grails.converters.JSON
import grails.util.Holders

/**
 * Created by hoyoux on 25.09.15.
 */
abstract class BioFormatConvertable extends Format implements IConvertableImageFormat {
    @Override
    def convert() {
        if(!Boolean.parseBoolean(Holders.config.bioformat.application.enabled)) throw new Exception("Convertor BioFormat not enabled");

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

            out.println('{path:"'+absoluteFilePath+'",group:false}');
            String result = inp.readLine();
            def json  = JSON.parse(result);
            files = json.files
            error = json.error;
        } catch (UnknownHostException e) {
            System.err.println(e.toString());
        }

        println "bioformat returns"
        println files

        if(files ==[] || files == null) {
            if (error != null) {
                throw new Exception("BioFormat Exception : \n"+error);
            }
        }
        return files
    }
}
