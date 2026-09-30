package pt.ipc_app.mlkit.vision

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class VideoTooLargeException : IOException("Video exceeds upload limit")

/** Only the upload copy is transcoded. The original remains available for recovery and Gallery. */
@android.annotation.SuppressLint("UnsafeOptInUsageError")
class CameraVideoUpload(private val context: Context) {
    suspend fun prepare(original: File): File {
        check(original.isFile && original.length() > 0)
        if (!VideoUploadPolicy.needsCompression(original.length())) return original
        val duration = withContext(Dispatchers.IO) {
            val metadata = MediaMetadataRetriever()
            try {
                metadata.setDataSource(original.absolutePath)
                metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                    ?: throw IOException("Missing video duration")
            } finally { metadata.release() }
        }
        val output = File(context.cacheDir, "upload_${UUID.randomUUID()}.mp4")
        try {
            transcode(original, output, VideoUploadPolicy.bitrate(duration))
            if (output.length() == 0L) throw IOException("Empty encoded video")
            if (VideoUploadPolicy.needsCompression(output.length())) throw VideoTooLargeException()
            return output
        } catch (e: Exception) {
            output.delete()
            throw e
        }
    }

    private suspend fun transcode(input: File, output: File, bitrate: Int) = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine<Unit> { continuation ->
            val transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setEncoderFactory(DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(bitrate).build()).build())
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        if (continuation.isActive) continuation.resume(Unit)
                    }
                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                }).build()
            continuation.invokeOnCancellation { transformer.cancel() }
            val edited = EditedMediaItem.Builder(MediaItem.fromUri(Uri.fromFile(input)))
                .setRemoveAudio(true)
                .setEffects(Effects(emptyList(), listOf(Presentation.createForHeight(480))))
                .build()
            transformer.start(edited, output.absolutePath)
        }
    }
}
