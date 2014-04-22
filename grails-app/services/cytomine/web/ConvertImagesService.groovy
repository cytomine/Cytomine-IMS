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

    private def rawFormatsAccepted = ["jp2", "svs", "scn", "mrxs", "ndpi", "vms", "bif", "zvi"]
    private def archiveFormatsAccepted = ["zip"]
    private def formatsToConvert = ["jpg", "jpeg", "png", "tiff", "tif", "pgm",  "bmp"]

    static String MRXS_EXTENSION = "mrxs"
    static String VMS_EXTENSION = "vms"

    static transactional = true

    def convertUploadedFile(Cytomine cytomine,UploadedFile uploadedFile) {
        //Check if file mime is allowed
        def allMime = rawFormatsAccepted.plus(formatsToConvert).plus(archiveFormatsAccepted)

        if (!allMime.contains(uploadedFile.get("ext"))) {
            log.info uploadedFile.get("filename") + " : FORMAT NOT ALLOWED"
            uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.ERROR_FORMAT,false)
            return [uploadedFile]
        }

        if (archiveFormatsAccepted.contains(uploadedFile.get("ext"))) {
            return handleCompressedFile(cytomine,uploadedFile)
        } else {
            return handleSingleFile(cytomine, uploadedFile)
        }
    }

    private def handleCompressedFile(Cytomine cytomine,UploadedFile uploadedFile) {
        /* Unzip the archive within the target */
        String destPath = zipService.uncompress(uploadedFile.getAbsolutePath())

        /* List files from the archive */
        def pathsAndExtensions = fileSystemService.getAbsolutePathsAndExtensionsFromPath(destPath)
        uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.UNCOMPRESSED,false)

        def specialFiles = handleSpecialFile(cytomine,uploadedFile,  pathsAndExtensions)
        if (specialFiles) return specialFiles

        //it looks like we have a set of "single file"
        def uploadedFiles = []
        pathsAndExtensions.each { it ->
            println it
            UploadedFile new_uploadedFile = createNewUploadedFile(cytomine, uploadedFile, it, null)
            uploadedFiles << new_uploadedFile

            UploadedFile converted_uploadedFile = handleSingleFile(cytomine, new_uploadedFile)

            if (converted_uploadedFile != new_uploadedFile) {
                uploadedFiles << converted_uploadedFile
            }
        }

        return uploadedFiles
    }

    private def handleSpecialFile(Cytomine cytomine,UploadedFile uploadedFile,  def pathsAndExtensions) {

        UploadedFile mainUploadedFile = null //mrxs or vms file
        def uploadedFiles = [] //nested files

        pathsAndExtensions.each { it ->
            if (it.extension == MRXS_EXTENSION || it.extension == VMS_EXTENSION) {
                mainUploadedFile = createNewUploadedFile(cytomine,uploadedFile, it,  null)
                mainUploadedFile = cytomine.editUploadedFile(mainUploadedFile.id,Cytomine.UploadStatus.TO_DEPLOY,true,mainUploadedFile.id)
            }
        }

        if (!mainUploadedFile) return null //ok, it's not a special file

        //create nested file

        uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.TO_DEPLOY,true)
        uploadedFiles << mainUploadedFile
        pathsAndExtensions.each { it ->
            if (it.extension != MRXS_EXTENSION && it.extension != VMS_EXTENSION) {
                UploadedFile nestedUploadedFile = createNewUploadedFile(cytomine,uploadedFile, it,  "application/octet-stream")
                uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.CONVERTED,true,mainUploadedFile.id)
                uploadedFiles << nestedUploadedFile
            }
        }
        return uploadedFiles
    }

    private UploadedFile createNewUploadedFile(Cytomine cytomine, def parentUploadedFile, def pathAndExtension, def contentType){

        String absolutePath = pathAndExtension.absolutePath
        String extension = pathAndExtension.extension
        String filename = absolutePath.substring(parentUploadedFile.getStr("path").length(), absolutePath.length())
        if (!contentType) {
            MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
            contentType = mimeTypesMap.getContentType(absolutePath)
        }

        return cytomine.addUploadedFile(
                new File(filename).getName(),
                filename,
                parentUploadedFile.getStr("path"),
                new File(absolutePath).size(),
                extension,
                contentType,
                parentUploadedFile.getList("projects"),
                parentUploadedFile.getList("storages"),
                parentUploadedFile.getLong("user"))
    }

    private boolean specialCheckVentana_TIFF(UploadedFile uploadedFile) {
        //check extension
        if (uploadedFile.get("ext") != "tif" && uploadedFile.get("ext") != "tiff") return false

        //try tiffinfo
        String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)

        String tiffinfo = "tiffinfo $originalFilenameFullPath".execute().text
        println tiffinfo
        return tiffinfo.contains("<iScan")

    }

    private UploadedFile handleSingleFile(Cytomine cytomine, UploadedFile uploadedFile) {

        //Check if file must be converted or not...
        if (!formatsToConvert.contains(uploadedFile.getStr("ext"))) {
            log.info uploadedFile.getStr("filename") + " : TO_DEPLOY"
            uploadedFile = cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.TO_DEPLOY,false,uploadedFile.id)
            return uploadedFile
        }

        boolean convertSuccessfull = true
        String convertFileName = uploadedFile.getStr("filename")
        convertFileName = convertFileName[0 .. (convertFileName.size() - uploadedFile.getStr("ext").size() - 2)]
        convertFileName = convertFileName + "_converted.tif"
        String originalFilenameFullPath = [ uploadedFile.getStr("path"), uploadedFile.getStr("filename")].join(File.separator)
        String convertedFilenameFullPath = [ uploadedFile.getStr("path"), convertFileName].join(File.separator)
        try {
            if (specialCheckVentana_TIFF(uploadedFile)) { //special check for TIFF file from ventana
                log.info "ventana TIFF detected"
                String biggestLayerFilename = uploadedFile.getStr("filename")
                biggestLayerFilename = biggestLayerFilename[0 .. (biggestLayerFilename.size() - uploadedFile.getStr("ext").size() - 2)]
                biggestLayerFilename = biggestLayerFilename + "_biggest_layer.tif"


                String biggestFilenameFullPath = [ uploadedFile.getStr("path"), biggestLayerFilename].join(File.separator)

                //1. Extract the biggest layer
                // vips im_vips2tiff 11GH076256_A2_CD3_100.tif:2 output_image.tif:deflate,,flat,,,,8
                def command = """vips im_vips2tiff $originalFilenameFullPath:2 $biggestFilenameFullPath:deflate,,flat,,,,8"""
                log.info "$command"
                convertSuccessfull &= ProcUtils.executeOnShell(command) == 0

                //2. Pyramid
                // vips tiffsave output_image.tif output_image_compress.tif --tile --pyramid --compression jpeg --tile-width 256 --tile-height 256
                command = """vips tiffsave $biggestFilenameFullPath $convertedFilenameFullPath --tile --pyramid --compression jpeg --tile-width 256 --tile-height 256"""
                log.info "$command"
                convertSuccessfull &= ProcUtils.executeOnShell(command)  == 0

                //3. Rm intermadiate file
                command = """rm $biggestFilenameFullPath"""
                convertSuccessfull &= ProcUtils.executeOnShell(command)  == 0

            } else {
                log.info "standard vips convert  $uploadedFile"

                convertSuccessfull = vipsify(originalFilenameFullPath, convertedFilenameFullPath)
            }
            if (convertSuccessfull) {

                UploadedFile convertUploadedFile = cytomine.addUploadedFile(
                        uploadedFile.getStr("originalFilename"),
                        convertFileName,
                        uploadedFile.getStr("path"),
                        new File(convertedFilenameFullPath).size(),
                        "tiff",
                        "image/tiff",
                        uploadedFile.getList("projects"),
                        uploadedFile.getList("storages"),
                        uploadedFile.getLong("user"),
                        Cytomine.UploadStatus.TO_DEPLOY)

                log.info "set uploaded parent file to UNCOMPRESSED"
                cytomine.editUploadedFile(uploadedFile.id,Cytomine.UploadStatus.CONVERTED,true)

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

    private def convert(String source, String target) {
        def command = """convert $source -define tiff:tile-geometry=256x256 -compress jpeg 'ptif:$target'"""
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

        // Thread.sleep(600000)

        log.info "$extractBandCommand"
        success &= (ProcUtils.executeOnShell(extractBandCommand) == 0)

        if(!success) {
            success = true
            log.info "$extractBandCommand"
            extractBandCommand = """$executable extract_band $source $intermediateFile[bigtiff,compression=lzw] 0 --n 1"""
            success &= (ProcUtils.executeOnShell(extractBandCommand) == 0)
        }

        log.info "$pyramidCommand"
        success &= (ProcUtils.executeOnShell(pyramidCommand) == 0)
        log.info "$rmIntermediatefile"
        success &= (ProcUtils.executeOnShell(rmIntermediatefile) == 0)






        return success
    }
}
