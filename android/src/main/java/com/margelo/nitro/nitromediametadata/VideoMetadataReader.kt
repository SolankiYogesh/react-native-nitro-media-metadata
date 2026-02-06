package com.margelo.nitro.nitromediametadata

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import android.webkit.URLUtil
import androidx.core.net.toUri
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

data class VideoLocation(
  val latitude: Double,
  val longitude: Double,
  val altitude: Double? = null
)

data class VideoMetadata(
  val duration: Double,
  val width: Int?,
  val height: Int?,
  val bitrate: Int?,
  val fileSize: Long?,
  val hasAudio: Boolean,
  val isHDR: Boolean?,
  val audioChannels: Int?,
  val audioSampleRate: Int?,
  val audioCodec: String?,
  val videoCodec: String?,
  val fps: Float?,
  val orientation: String,
  val naturalOrientation: String,
  val aspectRatio: Double?,
  val is16_9: Boolean,
  val location: VideoLocation?
)

class VideoMetadataReader(private val context: Context) {

  fun getVideoInfo(source: String, headers: Map<String, String>? = null): VideoMetadata? {
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

      // ---- Basic metadata ----
      val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
      val duration = BigDecimal(durationMs).divide(BigDecimal(1000), 3, RoundingMode.HALF_UP).toDouble()
      val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
      val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
      val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
      val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()
      val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null

      val colorTransfer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COLOR_TRANSFER)?.toIntOrNull()
      else null

      val isHDR = colorTransfer?.let {
        it == MediaFormat.COLOR_TRANSFER_ST2084 || it == MediaFormat.COLOR_TRANSFER_HLG
      }

      val location = extractGPSLocation(retriever)
      val orientation = getOrientation(rotation, width, height)

      var audioChannels: Int? = null
      var audioSampleRate: Int? = null
      var audioCodec: String? = null
      var videoCodec: String? = null
      var fps: Float? = null

      for (i in 0 until extractor.trackCount) {
        val format = extractor.getTrackFormat(i)
        val mimeType = format.getString(MediaFormat.KEY_MIME) ?: continue
        if (mimeType.startsWith("audio/")) {
          audioChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
          audioSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
          audioCodec = mapMimeTypeToCodecName(mimeType)
        } else if (mimeType.startsWith("video/")) {
          videoCodec = mapMimeTypeToCodecName(mimeType)
          fps = try {
            format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat()
          } catch (_: Exception) {
            try { format.getFloat(MediaFormat.KEY_FRAME_RATE) } catch (_: Exception) { null }
          }
        }
      }

      val aspectRatio = if (width != null && height != null && height != 0)
        width.toDouble() / height else null
      val is16_9 = aspectRatio?.let { abs(it - 16.0 / 9.0) < 0.01 } ?: false
      val naturalOrientation = if (width != null && height != null && height > width) "Portrait" else "Landscape"

      return VideoMetadata(
        duration, width, height, bitrate, fileSize, hasAudio, isHDR,
        audioChannels, audioSampleRate, audioCodec, videoCodec, fps,
        orientation, naturalOrientation, aspectRatio, is16_9, location
      )
    } catch (e: Exception) {
      Log.e("VideoMetadataReader", "Error reading metadata", e)
      return null
    } finally {
      retriever.release()
      extractor.release()
    }
  }

  private fun extractGPSLocation(retriever: MediaMetadataRetriever): VideoLocation? {
    val raw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION) ?: return null
    val cleaned = raw.trim('+', '/')
    val parts = cleaned.split("+")
    if (parts.size >= 2) {
      val lat = parts[0].toDoubleOrNull()
      val lon = parts[1].toDoubleOrNull()
      val alt = if (parts.size >= 3) parts[2].toDoubleOrNull() else null
      if (lat != null && lon != null) return VideoLocation(lat, lon, alt)
    }
    return null
  }

  private fun getOrientation(rotation: Int?, width: Int?, height: Int?): String {
    if (width == null || height == null || width == 0 || height == 0) return "LandscapeRight"
    val isPortrait = height > width
    val normalized = ((rotation ?: 0) % 360 + 360) % 360
    return when (normalized) {
      0 -> if (isPortrait) "Portrait" else "LandscapeRight"
      90 -> "Portrait"
      180 -> if (isPortrait) "PortraitUpsideDown" else "LandscapeLeft"
      270 -> "PortraitUpsideDown"
      else -> if (isPortrait) "Portrait" else "LandscapeRight"
    }
  }

  private fun mapMimeTypeToCodecName(mime: String): String = when {
    mime.startsWith("audio/") -> when {
      mime.contains("mp4a-latm") -> "aac"
      mime.contains("ac3") -> "ac3"
      mime.contains("opus") -> "opus"
      mime.contains("vorbis") -> "vorbis"
      mime.contains("flac") -> "flac"
      else -> mime.substringAfter("audio/")
    }
    mime.startsWith("video/") -> when {
      mime.contains("avc") || mime.contains("h264") -> "avc1"
      mime.contains("hev") || mime.contains("h265") -> "hev1"
      mime.contains("vp9") -> "vp9"
      mime.contains("vp8") -> "vp8"
      mime.contains("mp4v-es") -> "mp4v"
      else -> mime.substringAfter("video/")
    }
    else -> mime
  }
}
