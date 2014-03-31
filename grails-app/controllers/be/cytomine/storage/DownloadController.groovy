package be.cytomine.storage

import be.cytomine.client.Cytomine
import be.cytomine.client.models.AbstractImage

class DownloadController {

    def cytomineService

    private def responseFile(File file) {
        response.setHeader "Content-disposition", "attachment; filename=\"${file.getName()}\"";
        response.outputStream << file.newInputStream();
        response.outputStream.flush();
    }

    def download() {
        Cytomine cytomine = cytomineService.getCytomine(params.cytomineUrl)
        Long id = params.long("id")
        AbstractImage abstractImage = cytomine.getAbstractImage(id)
        String fullPath = abstractImage.getAt("fullPath")
        responseFile(new File(fullPath))
    }
}
