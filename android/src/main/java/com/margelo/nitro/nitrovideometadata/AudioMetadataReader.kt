package com.margelo.nitro.nitrovideometadata

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.webkit.URLUtil
import androidx.core.net.toUri
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode

data class AudioMetadata(
    val duration: Double,
    val fileSize: Long?,
    val codec: String?,
    val sampleRate: Int?,
    val channels: Int?,
    val bitRate: Int?,
    val artist: String?,
    val title: String?,
    val album: String?
)

class AudioMetadataReader(private val context: Context) {
    fun getAudioInfo(source: String, headers: Map<String, String>? = null): AudioMetadata? {
        val retriever = MediaMetadataRetriever()
        val extractor = MediaExtractor()
        try {
            val uri = source.toUri()
            var fileSize: Long? = null

            when {
                URLUtil.isFileUrl(source) -> {
                    val path = uri.path ?: return null
                    retriever.setDataSource(path)
                    extractor.setDataSource(path)
                    fileSize = File(path).length()
                }
                URLUtil.isContentUrl(source) -> {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
                        retriever.setDataSource(fd.fileDescriptor)
                        extractor.setDataSource(fd.fileDescriptor)
                    }
                }
                else -> {
                    retriever.setDataSource(source, headers ?: emptyMap())
                    extractor.setDataSource(source, headers ?: emptyMap())
                }
            }

            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val duration = BigDecimal(durationMs).divide(BigDecimal(1000), 3, RoundingMode.HALF_UP).toDouble()
            val bitRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

            var channels: Int? = null
            var sampleRate: Int? = null
            var codec: String? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mimeType = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mimeType.startsWith("audio/")) {
                    channels = try { format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) } catch (_: Exception) { null }
                    sampleRate = try { format.getInteger(MediaFormat.KEY_SAMPLE_RATE) } catch (_: Exception) { null }
                    codec = mapMimeTypeToCodecName(mimeType)
                    break
                }
            }

            return AudioMetadata(
                duration, fileSize, codec, sampleRate, channels, bitRate, artist, title, album
            )
        } catch (e: Exception) {
            return null
        } finally {
            retriever.release()
            extractor.release()
        }
    }

    private fun mapMimeTypeToCodecName(mime: String): String = when {
        mime.contains("mp4a-latm") -> "aac"
        mime.contains("ac3") -> "ac3"
        mime.contains("opus") -> "opus"
        mime.contains("vorbis") -> "vorbis"
        mime.contains("flac") -> "flac"
        else -> mime.substringAfter("audio/")
    }
}
