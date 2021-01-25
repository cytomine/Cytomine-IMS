package be.cytomine.formats.heavyconvertable.video

import be.cytomine.formats.tools.detectors.FfmpegDetector
import org.codehaus.groovy.control.CompilationFailedException
import utils.MimeTypeUtils
import utils.PropertyUtils

class MP4Format extends VideoFormat implements FfmpegDetector {

    String FFPROBE_FORMAT_IDENTIFIER = "mp4"

    MP4Format() {
        extensions = ["mp4"]
        mimeType = MimeTypeUtils.MIMETYPE_MP4

        cytominePropertyKeys[PropertyUtils.CYTO_WIDTH] = "Ffmpeg.streams[0].width"
        cytominePropertyKeys[PropertyUtils.CYTO_HEIGHT] = "Ffmpeg.streams[0].height"
        cytominePropertyKeys[PropertyUtils.CYTO_DURATION] = "Ffmpeg.streams[0].nb_frames"
        cytominePropertyKeys[PropertyUtils.CYTO_FPS] = "Ffmpeg.streams[0].r_frame_rate"

        cytominePropertyParsers[PropertyUtils.CYTO_FPS] = { x ->
            try {
                return PropertyUtils.parseDouble(Eval.me(x) ?: "")
            }
            catch (CompilationFailedException e) {
                return null
            }

        }
    }

}
