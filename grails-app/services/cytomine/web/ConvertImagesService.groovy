package cytomine.web

import be.cytomine.client.Cytomine
import be.cytomine.client.models.UploadedFile
import utils.ProcUtils

import javax.activation.MimetypesFileTypeMap

/**
 * TODOSTEVBEN:: doc + refactoring + security (?)
 */
class ConvertImagesService {

    def zipService
    def fileSystemService
    def grailsApplication



    static String MRXS_EXTENSION = "mrxs"
    static String VMS_EXTENSION = "vms"

    static transactional = true

    def convertUploadedFile(Cytomine cytomine,UploadedFile uploadedFile, def currentUserId,def allowedMime,def mimeToConvert,def zipMime) {
        //Check if file mime is allowed
        def allMime = allowedMime.plus(mimeToConvert).plus(zipMime)

        if (!allMime.contains(uploadedFile.get("ext"))) {
            log.info uploadedFile.get("filename") + " : FORMAT NOT ALLOWED"
            uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.ERROR_FORMAT,false)
            return [uploadedFile]
        }

        if (zipMime.contains(uploadedFile.get("ext"))) {
            return handleCompressedFile(cytomine,uploadedFile, currentUserId,mimeToConvert)
        } else {
            return handleSingleFile(cytomine,uploadedFile, currentUserId,mimeToConvert)
        }
    }

    private def handleCompressedFile(Cytomine cytomine,UploadedFile uploadedFile, def currentUserId, def mimeToConvert) {
        /* Unzip the archive within the target */
        String destPath = zipService.uncompress(uploadedFile.getAbsolutePath())

        /* List files from the archive */
        def pathsAndExtensions = fileSystemService.getAbsolutePathsAndExtensionsFromPath(destPath)
        uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.UNCOMPRESSED,false)

        def specialFiles = handleSpecialFile(cytomine,uploadedFile, currentUserId, pathsAndExtensions)
        if (specialFiles) return specialFiles

        //it looks like we have a set of "single file"
        def uploadedFiles = []
        pathsAndExtensions.each { it ->

            UploadedFile new_uploadedFile = createNewUploadedFile(uploadedFile, it, currentUserId, null)
            uploadedFiles << new_uploadedFile

            UploadedFile converted_uploadedFile = handleSingleFile(new_uploadedFile, currentUserId, mimeToConvert)

            if (converted_uploadedFile != new_uploadedFile && converted_uploadedFile.get("status") == Cytomine.UploadStatus.TO_DEPLOY) {
                uploadedFiles << converted_uploadedFile
            }
        }

        return uploadedFiles
    }

    private def handleSpecialFile(Cytomine cytomine,UploadedFile uploadedFile, def currentUserId, def pathsAndExtensions) {

        UploadedFile mainUploadedFile = null //mrxs or vms file
        def uploadedFiles = [] //nested files

        pathsAndExtensions.each { it ->
            if (it.extension == MRXS_EXTENSION || it.extension == VMS_EXTENSION) {
                mainUploadedFile = createNewUploadedFile(cytomine,uploadedFile, it, currentUserId, null)
                mainUploadedFile = cytomine.editUploadedFile(mainUploadedFile.id,Cytomine.UploadStatus.TO_DEPLOY,true,mainUploadedFile.id)
            }
        }

        if (!mainUploadedFile) return null //ok, it's not a special file

        //create nested file

        uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.TO_DEPLOY,true)
        uploadedFiles << mainUploadedFile
        pathsAndExtensions.each { it ->
            if (it.extension != MRXS_EXTENSION && it.extension != VMS_EXTENSION) {
                UploadedFile nestedUploadedFile = createNewUploadedFile(cytomine,uploadedFile, it, currentUserId, "application/octet-stream")
                uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.CONVERTED,true,mainUploadedFile.id)
                uploadedFiles << nestedUploadedFile
            }
        }
        return uploadedFiles
    }

    private UploadedFile createNewUploadedFile(Cytomine cytomine,UploadedFile parentUploadedFile, def pathAndExtension, def currentUserId, String contentType){
        String absolutePath = pathAndExtension.absolutePath
        String extension = pathAndExtension.extension
        String filename = absolutePath.substring(parentUploadedFile.getStr("path").length(), absolutePath.length())
        if (!contentType) {
            MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
            contentType = mimeTypesMap.getContentType(absolutePath)
        }

        return cytomine.addUploadedFile(parentUploadedFile.getStr("originalFilename"),filename,parentUploadedFile.getStr("path"),new File(absolutePath).size(),extension,contentType,parentUploadedFile.getList("projects"),parentUploadedFile.getList("storages"),currentUserId)
    }

    private UploadedFile handleSingleFile(Cytomine cytomine,UploadedFile uploadedFile, def currentUserId, def mimeToConvert) {

        //Check if file must be converted or not...
        if (!mimeToConvert.contains(uploadedFile.getStr("ext"))) {
            log.info uploadedFile.getStr("filename") + " : TO_DEPLOY"
            uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.TO_DEPLOY,false,uploadedFile.id)
            return uploadedFile

        } else {
            log.info "convert $uploadedFile"
            //..if yes. Convert it
            String convertFileName = uploadedFile.getStr("filename")
            convertFileName = convertFileName[0 .. (convertFileName.size() - uploadedFile.getStr("ext").size() - 2)]
            convertFileName = convertFileName + "_converted.tif"

            String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)
            String convertedFilenameFullPath = [ uploadedFile.getStr("path"), convertFileName].join(File.separator)


            try {


                Boolean success = false
                if (uploadedFile.getStr("ext") == "bif") { //bad use STATIC STRING FOR FORMAT
                    String bif2tifFilename = originalFilenameFullPath.replaceAll(".bif", ".tif")
                    String layer2bif2tifFilename = originalFilenameFullPath.replaceAll(".bif", "_layer2.tif")
                    rename(originalFilenameFullPath, bif2tifFilename)
                    success = extractLayer(bif2tifFilename, layer2bif2tifFilename, 2)
                    rename(bif2tifFilename, originalFilenameFullPath)
                    success = convert(layer2bif2tifFilename, convertedFilenameFullPath)
                } else {
                    success = vipsify(originalFilenameFullPath, convertedFilenameFullPath)
                }

                if (success) {

                    UploadedFile convertUploadedFile = cytomine.addUploadedFile(
                            uploadedFile.getStr("originalFilename"),
                            convertFileName,
                            uploadedFile.getStr("path"),
                            new File(convertedFilenameFullPath).size(),
                            "tiff",
                            "image/tiff",
                            uploadedFile.getList("projects"),
                            uploadedFile.getList("storages"),
                            currentUserId,
                            Cytomine.UploadStatus.TO_DEPLOY)


                    uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.CONVERTED,true)

                    return convertUploadedFile
                } else {
                    log.error "Vipsify cannot be done! Not a success!"
                    uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.ERROR_CONVERT,false)
                    return uploadedFile
                }

            } catch (Exception e) {
                e.printStackTrace()
                uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.ERROR_FORMAT)
                return uploadedFile
            }

        }

    }

    private def convert(String source, String target) {
        def command = """convert $source -define tiff:tile-geometry=256x256 -compress jpeg 'ptif:$target'"""
        log.info "$command"
        return ProcUtils.executeOnShell(command) == 0

    }

    private def rename(String source, String target) {
        def executable = "mv"
        def command = """$executable $source $target"""
        log.info "$command"
        return ProcUtils.executeOnShell(command) == 0
    }
    
    private def extractLayer(String source, String target, Integer layer) {
        //1. Look for vips executable
        println new File(source).exists()
        def executable = "convert"
        if (System.getProperty("os.name").contains("OS X")) {
            executable = "/usr/local/bin/convert"
        }
        log.info "convert is in : $executable"

        def command = """$executable $source[$layer] $target"""

        log.info "$command"


        return ProcUtils.executeOnShell(command) == 0
    }

    private def vipsify(String source, String target) {
        //1. Look for vips executable
        println new File(source).exists()
        def executable = "/usr/local/bin/vips"
        if (System.getProperty("os.name").contains("OS X")) {
            executable = "/usr/local/bin/vips"
        }
        def intermediateFile = target.replace(".tif",".tmp.tif")

        log.info "vips is in : $executable"

        def extractBandCommand = """$executable extract_band $source $intermediateFile[bigtiff,compression=lzw] 0 --n 3"""
        def rmIntermediatefile = """rm $intermediateFile"""
        def pyramidCommand = """$executable tiffsave "$intermediateFile" "$target" --tile --pyramid --compression lzw --tile-width 256 --tile-height 256 --bigtiff"""

        boolean success = true

        log.info "$extractBandCommand"
        success &= (ProcUtils.executeOnShell(extractBandCommand) == 0)
        log.info "$pyramidCommand"
        success &= (ProcUtils.executeOnShell(pyramidCommand) == 0)
        log.info "$rmIntermediatefile"
        success &= (ProcUtils.executeOnShell(rmIntermediatefile) == 0)

        return success
    }
}
