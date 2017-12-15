package ims

import be.cytomine.client.Cytomine
import be.cytomine.client.models.UploadedFile
import org.apache.commons.io.FileUtils


class DeleteImageFileJob {
    static triggers = {
        cron name: "DeleteImageTrigger", cronExpression: "0 0 1 * * ?"// execute job at 1 AM every day
    }

    def grailsApplication

    def execute() {
        // execute job
        String cytomineUrl = grailsApplication.config.cytomine.coreURL

        String pubKey = grailsApplication.config.cytomine.imageServerPublicKey
        String privKey = grailsApplication.config.cytomine.imageServerPrivateKey

        Cytomine cytomine = new Cytomine((String) cytomineUrl, pubKey, privKey)

        cytomine.testHostConnection();

        def files = cytomine.getUploadedFiles(true);

        for(int i = 0; i<files.size(); i++) {
            UploadedFile file = files.get(i)

            if ((new Date().time)-Long.parseLong(file.get("deleted")) >= 24*60*60*1000 && (new Date().time)-Long.parseLong(file.get("deleted")) < 48*60*60*1000){

                File fileToDelete = new File(file.getAbsolutePath())
                if(fileToDelete.exists()) {
                    fileToDelete.delete();
                    /*fileToDelete = fileToDelete.parentFile
                    // delete the folder if no more file exists
                    if(fileToDelete.listFiles().collect{it.isDirectory()}.size() == fileToDelete.listFiles().size()){
                        FileUtils.deleteDirectory(fileToDelete);
                    }*/
                }
            }
        }
    }
}
