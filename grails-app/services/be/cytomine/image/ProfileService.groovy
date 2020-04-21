package be.cytomine.image

/*
 * Copyright (c) 2009-2019. Authors: see NOTICE file.
 *
 * Licensed under the GNU Lesser General Public License, Version 2.1 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.gnu.org/licenses/lgpl-2.1.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import be.cytomine.client.CytomineConnection
import be.cytomine.client.collections.Collection
import be.cytomine.client.models.*
import be.cytomine.exception.DeploymentException
import be.cytomine.exception.MiddlewareException
import be.cytomine.formats.FormatIdentifier
import be.cytomine.formats.supported.JPEG2000Format
import be.cytomine.formats.tools.CytomineFile
import ch.systemsx.cisd.base.mdarray.MDByteArray
import ch.systemsx.cisd.base.mdarray.MDIntArray
import ch.systemsx.cisd.base.mdarray.MDShortArray
import ch.systemsx.cisd.hdf5.*
import com.vividsolutions.jts.geom.Coordinate
import com.vividsolutions.jts.geom.Envelope
import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.geom.GeometryFactory
import com.vividsolutions.jts.geom.util.AffineTransformation
import com.vividsolutions.jts.operation.predicate.RectangleIntersects
import groovyx.gpars.dataflow.DataflowQueue
import groovyx.gpars.dataflow.operator.PoisonPill
import groovyx.gpars.group.DefaultPGroup
import groovyx.gpars.scheduler.DefaultPool
import hdf.hdf5lib.exceptions.HDF5SymbolTableException
import org.codehaus.groovy.grails.web.util.TypeConvertingMap
import utils.FilesUtils
import utils.ImageUtils
import utils.MimeTypeUtils

import javax.imageio.ImageIO
import java.awt.*
import java.awt.image.BufferedImage
import java.nio.file.Paths

import static ch.systemsx.cisd.hdf5.MatrixUtils.dims

class ProfileService {
    def fileSystemService
    def cytomineService
    def imageProcessingService
    def grailsApplication

    final String HDF5_DATASET = "data"
    final String HDF5_EXTENSION = "hdf5"
    final String HDF5_FILENAME = "profile.${HDF5_EXTENSION}"

    /**
     * Create a profile as companion file for the given 3D grayscale image.
     *
     * @param userConn              The connection used to communicate with core as regular user
     * @param uploadedFileParentId  The id of the uploaded file parent for the companion file
     * @param abstractImageId       The id of the abstract image for which the profile is computed
     * @return A map with a companion file and its uploaded file (with UPLOADED status)
     */
    def create(CytomineConnection userConn, Long uploadedFileParentId, Long abstractImageId) {
        def parent = new UploadedFile().fetch(userConn, uploadedFileParentId)
        def image = new AbstractImage().fetch(userConn, abstractImageId)
        def storage = new Storage().fetch(userConn, parent.getLong("storage"))

        def imsServer = cytomineService.getThisImageServer()
        if (!imsServer)
            throw new MiddlewareException("IMS reference not found in core.")

        def size = 0L
        def filename = HDF5_FILENAME
        def extension = HDF5_EXTENSION
        def status = UploadedFile.Status.UPLOADED
        def contentType = MimeTypeUtils.MIMETYPE_HDF5
        def projects = new Collection(Project.class, 0, 0)
        def destinationPath = Paths.get(new Date().getTime().toString(), FilesUtils.correctFilename(filename))

        def uploadedFile = new UploadedFile(imsServer, filename, destinationPath.toString(), size, extension,
                contentType, projects, storage, userConn.getCurrentUser(), status, parent).save(userConn)

        def companionFile = new CompanionFile(uploadedFile, image, filename, filename, "HDF5").save(userConn)

        Collection<AbstractSlice> slices = new Collection<>(AbstractSlice.class, 0, 0)
        slices.addFilter("abstractimage", companionFile.getLong("image") as String)
        slices.fetch(userConn)

        runAsync {
            try {
                convert(uploadedFile, image, slices)
            }
            catch (Exception e) {
                e.printStackTrace()
            }
        }

        return [
                uploadedFile : uploadedFile,
                companionFile: companionFile
        ]
    }

    /**
     * Convert a list of slices to a HDF5 file for profile extraction
     *
     * @param uploadedFile  The UF for which progress is updated
     * @param image         The abstract image for which profile is computed. Must be 3D greyscale image.
     * @param slices        The list of slices of the abstract image
     */
    def convert(UploadedFile uploadedFile, AbstractImage image, Collection<AbstractSlice> slices) {
        try {
            uploadedFile.changeStatus(UploadedFile.Status.CONVERTING)

            File file = new File((String) uploadedFile.get('path'))
            fileSystemService.makeLocalDirectory(file)

            def useJpegTiles = false

            def imageWidth = image.getInt("width")
            def imageHeight = image.getInt("height")
            def nZooms = image.getStr("zoom")

            def bpc = image.getInt("bitPerSample") ?: 8
            def dimension = null
            if (image.getInt("channels") > 1) dimension = 'channel'
            if (image.getInt("depth") > 1) dimension = 'zStack'
            if (image.getInt("duration") > 1) dimension = 'time'

            if (!dimension) {
                throw new DeploymentException("Cannot make profile for 2D image " + image)
            }

            int nSlices = slices.size()

            Integer tileSize = grailsApplication.config.cytomine.ims.tile.size
            def xTiles = Math.ceil(image.getDbl("width") / (double) tileSize)
            def yTiles = Math.ceil(image.getDbl("height") / (double) tileSize)
            def nTilesPerSlice = xTiles * yTiles

            def nTiles = nSlices * nTilesPerSlice
            def nExtractorThreads = 4 // Should be put as config BUT issue with terminator condition (PoisonPill)
            def nTilesPerThread = Math.ceil((double) nTiles / (double) nExtractorThreads)

            def tiles = []
            for (int i = 0; i < nSlices; i++) {
                AbstractSlice a = slices.get(i)
                def path = a.getStr("path")
                def mimeType = a.getStr("mime")
                def position = a.getInt(dimension)

                def imageFormat = new FormatIdentifier(new CytomineFile(path)).identify(mimeType, true)
                (0..(xTiles - 1)).each { x ->
                    (0..(yTiles - 1)).each { y ->
                        tiles << [
                                X        : x,
                                Y        : y,
                                tileIndex: x + (y * xTiles),
                                slice    : position,
                                format   : imageFormat,
                                url: path
                        ]
                    }
                }
            }

            final def queue = new DataflowQueue()
            Integer queueLimit = grailsApplication.config.cytomine.ims.hdf5.queueLimit
            def group = new DefaultPGroup(new DefaultPool(true, nExtractorThreads + 1))

            // JHDF5 example: http://svnsis.ethz.ch/doc/hdf5/hdf5-19.04/ch/systemsx/cisd/hdf5/examples/BlockwiseMatrixExample.java
            // JHDF5 doc: http://svnsis.ethz.ch/doc/hdf5/hdf5-19.04/

            def consumer = group.task {
                IHDF5Writer hdf5 = null
                try {
                    hdf5 = HDF5Factory.open(file)

                    hdf5.int32().write("version", 2)
                    hdf5.int32().write("width", image.getInt("width"))
                    hdf5.int32().write("height", image.getInt("height"))
                    hdf5.int32().write("bpc", bpc)
                    hdf5.int32().write("blockSize", tileSize)
                    hdf5.int32().write("nSlices", nSlices)
                    hdf5.string().write("dimension", dimension)

                    HDF5DataSet dataset = null
                    try {
                        HDF5IntStorageFeatures features = HDF5IntStorageFeatures.INT_CHUNKED
                        def dimensions = dims(tileSize, tileSize, nSlices)
                        dataset = getHDF5Writer(hdf5, bpc)?.createMDArrayAndOpen(HDF5_DATASET, dimensions, features)

                        // The array has the right type depending on 'bpc' and is filled at each tile iteration
                        def array = getTileMDArray((int[]) dims(tileSize, tileSize), bpc)

                        while (true) {
                            def tile = queue.getVal()
                            log.info "Write tile ${tile.tileIndex} (slice ${tile.slice})"
                            try {
                                fillTileMDArray(array, tile.data, bpc)
                                long[] indexes = [tile.Y, tile.X, 0] // Reverse X & Y as we are in a matrix-like referential
                                long[] sliceIndexes = [-1, -1, tile.slice] // We write a slice in the block, at given depth

                                getHDF5Writer(hdf5, bpc)?.writeSlicedMDArrayBlock(dataset, array, indexes, sliceIndexes)
                            }
                            catch (Exception e) {
                                log.error("Error while writing tile ${tile.tileIndex} (slice ${tile.slice}): $e")
                                e.printStackTrace()
                                throw e // Re-throw to update UploadedFile
                            }
                        }
                    }
                    finally { if (dataset != null) dataset.close() }
                }
                finally { if (hdf5 != null) hdf5.close() }
            }

            def producers = []
            (0..nExtractorThreads - 1).each { n ->
                producers << group.task {
                    int start = n * nTilesPerThread
                    int end = Math.max(start, Math.min((n + 1) * nTilesPerThread, nTiles) - 1)
                    (start..end).each { i ->
                        def tile = tiles[i]
                        def params = [:]

                        def url
                        if (bpc == 8 && useJpegTiles) {
                            params = [z: nZooms, tileIndex: tile.tileIndex]
                            url  = tile.format.tileURL(new TypeConvertingMap(params))
                        }
                        else {
                            params = [
                                    topLeftX: tile.X * tileSize, topLeftY: imageHeight - (tile.Y * tileSize),
                                    width: tileSize, height: tileSize,
                                    imageWidth: imageWidth, imageHeight: imageHeight,
                                    bits: bpc, format: 'png'
                            ]
                            url = tile.format.cropURL(new TypeConvertingMap(params))
                        }

                        try {
                            println url
                            BufferedImage bi = getImageFromURL(url)
                            if(tile.format instanceof JPEG2000Format) {
                                /*
                                 * When we ask a crop with size = w*h, we translate w to 1d/(imageWidth / width) for old IIP server request. Same for h.
                                 * We may loose precision and the size could be w+-1 * h+-1.
                                 * If the difference is < as threshold, we rescale
                                 */
                                def dimensions = ImageUtils.getComputedDimensions(new TypeConvertingMap(params))
                                if ((int) dimensions.computedWidth != bi.width || (int) dimensions.computedHeight != bi.height) {
                                    bi = imageProcessingService.scaleImage(bi, (int) dimensions.computedWidth, (int) dimensions.computedHeight)
                                }
                            }
                            log.info "Read tile ${tile.tileIndex} (slice ${tile.slice})"

                            int widthPadding = tileSize - bi.getWidth()
                            int heightPadding = tileSize - bi.getHeight()
                            if (widthPadding > 0 || heightPadding > 0) {
                                log.info("Pad tile  ${tile.tileIndex} (slice ${tile.slice}:  " +
                                        "width: $widthPadding , height: $heightPadding")
                                BufferedImage tmp = new BufferedImage(bi.getWidth() + widthPadding,
                                        bi.getHeight() + heightPadding, bi.getType())
                                Graphics2D g2 = tmp.createGraphics()
                                g2.drawImage(bi, 0, 0, null)
                                g2.dispose()

                                bi = tmp
                            }

                            // Raster.getPixels in giving RGB pixels (even for a grayscale image !)
                            // Use Raster.getSamples, with band 0 but return everything in integers (even for 8/16-bit images !)
                            // See: https://stackoverflow.com/questions/31312645/java-imageio-grayscale-png-issue
                            tile['data'] = bi.getData().getSamples(0, 0, tileSize, tileSize, 0, (int[]) null)
                            queue << tile

                            log.info("Added tile ${tile.tileIndex} (slice ${tile.slice}) in queue.")
                        }
                        catch (Exception e) {
                            log.error("Error while queuing tile ${tile.tileIndex} (slice ${tile.slice}): $e")
                            e.printStackTrace()
                            throw e // Re-throw to update UploadedFile
                        }

                        def maxRetries = 50
                        if (queue.length() > queueLimit) {
                            log.info "Pause producers so that consumer can consume a bit"
                            while (queue.length() > queueLimit / 2 && maxRetries > 0) {
                                sleep(500)
                                maxRetries--
                            }
                            log.info "Queue has been a bit cleared"
                        }
                    }
                }
            }

            // The operator's body must have the same number of parameters (t1, t2, t3, t4) as given in input !
            // TODO: find a way to dynamically assign the right number of parameters in the closure (Object... does not work)
            group.operator(inputs: producers, outputs: []) { t1, t2, t3, t4 ->
                queue << PoisonPill.instance
            }

            consumer.join() // Wait consumer has finished (i.e when it receives PoisonPill)
            group.shutdown()

            uploadedFile.set("size", file.size())
            uploadedFile.set("status", UploadedFile.Status.CONVERTED.code)
            uploadedFile.update()
        }
        catch (Exception e) {
            log.error(e)
            e.printStackTrace()
            uploadedFile.changeStatus(UploadedFile.Status.ERROR_CONVERSION)
            throw e
        }
    }

    /**
     * Find a profile for a point given in a cartesian coordinate system.
     *
     * @param path      The HDF5 filepath
     * @param x         The point coordinate along x-axis (horizontal, left to right)
     * @param y         The point coordinate along y-axis (vertical, bottom to top)
     * @param bounds    A map with 'min' and 'max' limiting the returned profile
     * @return          A map whose first item is point coordinate and second one a list of pixel values extracted from
     * HDF5 file for the given coordinates between the given bounds.
     */
    def pointProfile(String path, int x, int y, def bounds) {
        File f = new File(path)
        if (!f.exists()) {
            throw new FileNotFoundException(f.absolutePath + "does not exist.")
        }

        IHDF5Reader hdf5 = null
        try {
            hdf5 = HDF5Factory.openForReading(f)

            try {
                int version = hdf5.int32().read("version")
                return pointProfileV2(hdf5, x, y, bounds)
            }
            catch (HDF5SymbolTableException ignored){
                return pointProfileV1(hdf5, x, y, bounds)
            }
        }
        finally {
            if (hdf5 != null) hdf5.close()
        }
    }

    def pointProfileV2(IHDF5Reader hdf5, int x, int y, def bounds) {
        int imageWidth = hdf5.int32().read("width")
        int imageHeight = hdf5.int32().read("height")
        int bpc = hdf5.int32().read("bpc")
        int nSlices = hdf5.int32().read("nSlices")

        int minBound = (int) Math.max(0, bounds.min)
        int maxBound = (int) Math.min(nSlices, bounds.max)

        x = Math.min(x, imageWidth)
        y = Math.min(y, imageHeight)

        // We change referential for a matrix-like system used by HDF5
        int row = imageHeight - y
        int col = x

        def mask = getMask(bpc)
        def reader = getHDF5Reader(hdf5, bpc)
        def array = reader?.readMDArraySlice(HDF5_DATASET, (long[]) [row, col, -1L])

        def data = []
        for (int i = minBound; i < Math.min(array.size(), maxBound); i++) {
            data << (int) (array.get(i) & mask)
        }

        hdf5.close()

        return [point: [x, y], profile: data]
    }

    def pointProfileV1(IHDF5Reader hdf5, int x, int y, def bounds) {
        int[] meta = hdf5.int32().readArray("/meta")
        def blockLength = meta[0]
        def bpc = meta[1]
        def depth = meta[2]
        def imageWidth = meta[3]
        def imageHeight = meta[4]

        def minBound = Math.max(0, bounds.min)
        def maxBound = Math.min(depth, bounds.max)

        x = Math.min(x, imageWidth)
        y = Math.min(y, imageHeight)

        /* In V1, the input was
            - x is horizontal axis, left to right
            - y is vertical axis, top to bottom
           Now, the input is given by a cartesian coordinate system
         */
        def y2 = imageHeight - y

        def X = (int) Math.floor(x / blockLength)
        def Y = (int) Math.floor(y2 / blockLength)

        def xStart = X * blockLength
        def xEnd = Math.min(xStart + blockLength, imageWidth)
        def yStart = Y * blockLength
        def yEnd = Math.min(yStart + blockLength, imageHeight)
        def blockWidth = xEnd - xStart
        def blockHeight = yEnd - yStart

        def reader = getHDF5Reader(hdf5, bpc)
        def block = reader.readArray("/b${X}_${Y}")

        def mask = getMask(bpc)

        def data = []
        for (def k = minBound; k < maxBound; k++) {
            def i = x % blockWidth
            def j = y2 % blockHeight
            data << (int) (block[i + blockWidth * j + blockWidth * blockHeight * k] & mask)
        }

        return [point: [x, y], profile: data]
    }

    /**
     * Find a profile for a geometry given in a cartesian coordinate system.
     *
     * @param path      The HDF5 filepath
     * @param geometry  The valid geometry in a cartesian coordinate system
     * @param bounds    A map with 'min' and 'max' limiting the returned profile
     * @return          A list of maps whose first item is point coordinate and second one a list of pixel values extracted from
     * HDF5 file for the given coordinates between the given bounds.
     */
    def geometryProfile(String path, Geometry geometry, def bounds) {
        File f = new File(path)
        if (!f.exists()) {
            throw new FileNotFoundException(f.absolutePath + "does not exist.")
        }

        Envelope env = geometry.getEnvelopeInternal()
        int xleft = (int) Math.round(env.getMinX())
        int xright = (int) Math.round(env.getMaxX())
        int ytop = (int) Math.round(env.getMaxY())
        int ybottom = (int) Math.round(env.getMinY())
        int width = xright - xleft
        int height = ytop - ybottom

        IHDF5Reader hdf5 = null
        try {
            hdf5 = HDF5Factory.openForReading(f)

            try {
                int version = hdf5.int32().read("version")
                return geometryProfileV2(hdf5, xleft, ytop, width, height, geometry, bounds)
            }
            catch(HDF5SymbolTableException ignored) {
                return geometryProfileV1(hdf5, xleft, ytop, width, height, geometry, bounds)
            }
        }
        finally {
            if (hdf5 != null) hdf5.close()
        }
    }

    def geometryProfileV2(IHDF5Reader hdf5, int x, int y, int width, int height, Geometry geometry, def bounds) {
        int imageWidth = hdf5.int32().read("width")
        int imageHeight = hdf5.int32().read("height")
        int bpc = hdf5.int32().read("bpc")
        int nSlices = hdf5.int32().read("nSlices")
        int blockSize = hdf5.int32().read("blockSize")

        int minBound = (int) Math.max(0, bounds.min)
        int maxBound = (int) Math.min(nSlices, bounds.max)

        def xmin = Math.min(x, imageWidth)
        def xmax = Math.min(xmin + width, imageWidth)
        def ymax = Math.min(y, imageHeight)
        def ymin = Math.min(ymax - height, imageHeight)
        log.debug "X: $xmin - $xmax | Y: $ymin - $ymax"

        // We change referential for a matrix-like system used by HDF5
        int minRow = imageHeight - ymax
        int maxRow = imageHeight - ymin
        int minCol = xmin
        int maxCol = xmax
        log.debug "Row: $minRow - $maxRow | Col: $minCol - $maxCol"

        // Find min and max block to ask in row, col dimensions
        int minRowBlock = (int) Math.floor(minRow / blockSize)
        int maxRowBlock = (int) Math.ceil(maxRow / blockSize)
        int minColBlock = (int) Math.floor(minCol / blockSize)
        int maxColBlock = (int) Math.ceil(maxCol / blockSize)
        log.debug "RowBlock: $minRowBlock - $maxRowBlock | ColBlock: $minColBlock - $maxColBlock"

        def mask = getMask(bpc)
        def reader = getHDF5Reader(hdf5, bpc)
        int[] blockDimensions = [blockSize, blockSize, nSlices]

        GeometryFactory gf = new GeometryFactory()
        AffineTransformation at = new AffineTransformation(1.0, 0.0, 0.0, 0.0, -1.0, imageHeight)
        geometry = at.transform(geometry)

        def results = []
        for (int bx = minRowBlock; bx < maxRowBlock; bx++) {
            for (int by = minColBlock; by < maxColBlock; by++) {
                // We have to find the local (min, max) in row, col dimensions w.r.t. block to get only data in the bbox
                int blockOffsetRow = bx * blockSize
                int blockOffsetCol = by * blockSize
                log.debug "BlockOffsetRow: $blockOffsetRow | BlockOffsetCol: $blockOffsetCol"

                // Only read the block if the geometry intersects the block
                Geometry blockBbox = gf.toGeometry(new Envelope(blockOffsetCol, blockOffsetCol + blockSize - 1,
                        blockOffsetRow, blockOffsetRow + blockSize - 1))
                if (!RectangleIntersects.intersects(blockBbox, geometry)) {
                    log.debug "Block ($bx , $by) is skipped."
                    continue
                }

                log.debug "Reading block ($bx , $by)"
                def array = reader?.readMDArrayBlock(HDF5_DATASET, blockDimensions, (long[]) [bx, by, 0])

                int minLocalRow = (blockOffsetRow < minRow) ? (minRow % blockSize) : 0
                int maxLocalRow = (blockOffsetRow + blockSize > maxRow) ? (maxRow % blockSize): blockSize
                int minLocalCol = (blockOffsetCol < minCol) ? (minCol % blockSize) : 0
                int maxLocalCol = (blockOffsetCol + blockSize > maxCol) ? (maxCol % blockSize) : blockSize
                log.debug "LocalRow: $minLocalRow - $maxLocalRow | LocalCol: $minLocalCol - $maxLocalRow"

                // Only need precise point cover check if the whole block is not within the adjusted block bbox
                def blockWithinGeom = blockBbox.within(geometry)
                for (int i = minLocalRow; i < maxLocalRow; i++) {
                    for (int j = minLocalCol; j < maxLocalCol; j++) {
                        def pointX = blockOffsetCol + j
                        def pointY = blockOffsetRow + i
                        if (!blockWithinGeom && !geometry.covers(gf.createPoint(new Coordinate(pointX, pointY)))) {
                            continue
                        }

                        def data = []
                        for (int k = minBound; k < maxBound; k++) {
                            data << (int) (array.get(i, j, k) & mask)
                        }
                        results << [point: [pointX, imageHeight - pointY], profile: data]
                    }
                }
            }
        }

        hdf5.close()

        return results
    }

    def geometryProfileV1(IHDF5Reader hdf5, int x, int y, int width, int height, Geometry geometry, def bounds) {
        int[] meta = hdf5.int32().readArray("/meta")
        def blockLength = meta[0]
        def bpc = meta[1]
        def depth = meta[2]
        def imageWidth = meta[3]
        def imageHeight = meta[4]

        def minBound = Math.max(0, bounds.min)
        def maxBound = Math.min(depth, bounds.max)

        /* In V1, the input was
            - x is horizontal axis, left to right
            - y is vertical axis, top to bottom
           Now, the input is given by a cartesian coordinate system
         */
        y = imageHeight - y

        def mask = getMask(bpc)
        def reader = getHDF5Reader(hdf5, bpc)

        GeometryFactory gf = new GeometryFactory()
//        AffineTransformation at = new AffineTransformation(1.0, 0.0, 0.0, 0.0, -1.0, imageHeight)
//        geometry = at.transform(geometry)

        def xmin = Math.min(x, imageWidth)
        def ymin = Math.min(y, imageHeight)
        def xmax = Math.min(xmin + width + 1, imageWidth)
        def ymax = Math.min(ymin + height + 1, imageHeight)

        def Xmin = (int) Math.floor(xmin / blockLength)
        def Ymin = (int) Math.floor(ymin / blockLength)
        def Xmax = (int) Math.ceil(xmax / blockLength)
        def Ymax = (int) Math.ceil(ymax / blockLength)

        def spectrum = []
        for (def X = Xmin; X < Xmax; X++) {
            for (def Y = Ymin; Y < Ymax; Y++) {
                def xStart = X * blockLength
                def xEnd = Math.min(xStart + blockLength, imageWidth)
                def yStart = Y * blockLength
                def yEnd = Math.min(yStart + blockLength, imageHeight)
                def blockWidth = xEnd - xStart
                def blockHeight = yEnd - yStart

                // Only read the block if the geometry intersects the block
                Geometry blockBbox = gf.toGeometry(new Envelope(xStart, xEnd - 1, yStart, yEnd - 1))
                if (!RectangleIntersects.intersects(blockBbox, geometry)) {
                    log.debug "Block ($X , $Y) is skipped."
                    continue
                }

                def block = reader.readArray("/b${X}_${Y}")

                def x1 = (int) Math.max(xmin, xStart)
                def x2 = (int) Math.min(xmax, xEnd)
                def y1 = (int) Math.max(ymin, yStart)
                def y2 = (int) Math.min(ymax, yEnd)

                // Only need precise point cover check if the whole block is not within the adjusted block bbox
                def blockWithinGeom = blockBbox.within(geometry)
                for (def i = x1; i < x2; i++) {
                    for (def j = y1; j < y2; j++) {
                        if (!blockWithinGeom && !geometry.covers(gf.createPoint(new Coordinate(i, j)))) {
                            continue
                        }
                        def res = []
                        for (def k = minBound; k < maxBound; k++) {
                            def ii = i % blockWidth
                            def jj = j % blockHeight
                            res << (int) (block[ii + blockWidth * jj + blockWidth * blockHeight * k] & mask)
                        }
                        spectrum << [point: [i, j], profile: res]
                    }
                }
            }
        }

        return spectrum
    }

    private static def getHDF5Reader(IHDF5Reader hdf5, bpc) {
        if (bpc <= 8) return hdf5.int8()
        if (bpc <= 16) return hdf5.int16()

        return hdf5.int32()
    }

    private static def getMask(bpc) {
        if (bpc <= 8) return 0xFF
        if (bpc <= 16) return 0xFFFF

        return 0xFFFFFFFF
    }

    private static def getHDF5Writer(IHDF5Writer writer, int bpc) {
        if (bpc <= 8) return writer.int8()
        if (bpc <= 16) return writer.int16()

        return writer.int32()
    }

    private static def getTileMDArray(int[] dims, int bpc) {
        if (bpc <= 8) return new MDByteArray(dims)
        if (bpc <= 16) return new MDShortArray(dims)

        return new MDIntArray(dims)
    }

    private static def fillTileMDArray(def array, int[] data, int bpc) {
        if (bpc <= 8) {
            for (int i = 0; i < data.length; i++) {
                array.set((byte) data[i], i)
            }
        }
        else if (bpc <= 16) {
            for (int i = 0; i < data.length; i++) {
                array.set((short) data[i], i)
            }
        }
        else {
            for (int i = 0; i < data.length; i++) {
                array.set(data[i], i)
            }
        }
    }

    private static BufferedImage getImageFromURL(String url) {
        def out = new ByteArrayOutputStream()
        try {
            out << new URL(url).openStream()
        } catch (IOException e) {
            println "getImageFromURL $url Exception " + e.toString()
        }
        InputStream inputStream = new ByteArrayInputStream(out.toByteArray())
        BufferedImage bufferedImage = ImageIO.read(inputStream)
        inputStream.close()
        out.close()
        return bufferedImage
    }
}
