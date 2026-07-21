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
  val orientation: VideoOrientation,
  val naturalOrientation: NaturalOrientation,
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
          audioCodec = MediaMetadataUtils.mapMimeTypeToCodecName(mimeType)
        } else if (mimeType.startsWith("video/")) {
          videoCodec = MediaMetadataUtils.mapMimeTypeToCodecName(mimeType)
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
      val naturalOrientation = if (width != null && height != null && height > width) NaturalOrientation.PORTRAIT else NaturalOrientation.LANDSCAPE

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

  /**
   * Extracts a single representative video frame as a base64-encoded JPEG data URI.
   *
   * @param timeMs Time position (milliseconds) to grab the frame from. Defaults to the first frame.
   */
  fun getVideoThumbnail(source: String, headers: Map<String, String>? = null, timeMs: Long = 0L): String? {
    val retriever = MediaMetadataRetriever()
    try {
      val uri = source.toUri()

      when {
        URLUtil.isFileUrl(source) -> {
          val path = uri.path ?: return null
          retriever.setDataSource(path)
        }
        URLUtil.isContentUrl(source) -> {
          context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            retriever.setDataSource(fd.fileDescriptor)
          }
        }
        else -> {
          retriever.setDataSource(source, headers ?: emptyMap())
        }
      }

      val frame = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        ?: return null
      return MediaMetadataUtils.bitmapToJpegDataUri(frame)
    } catch (e: Exception) {
      Log.e("VideoMetadataReader", "Error extracting video thumbnail", e)
      return null
    } finally {
      retriever.release()
    }
  }

  private fun extractGPSLocation(retriever: MediaMetadataRetriever): VideoLocation? {
    val raw = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION) ?: return null
    return parseISO6709(raw)
  }

  /**
   * Parses an ISO6709 location string (e.g. "+37.5090-122.2364+000.000/") into lat/lon/altitude.
   *
   * The naive approach of stripping the leading sign and splitting on "+" only works when
   * longitude happens to be positive. Since ISO6709 always prefixes every component (lat, lon,
   * optional altitude) with an explicit "+" or "-" sign, we instead scan for all signed decimal
   * numbers directly - this correctly handles all four lat/lon sign combinations. Mirrors
   * `parseISO6709` in ios/VideoMetadata.swift.
   */
  private fun parseISO6709(raw: String): VideoLocation? {
    val cleaned = raw.replace("/", "")
    val regex = Regex("([+-]\\d+\\.\\d+)")
    val coords = regex.findAll(cleaned).map { it.value.toDouble() }.toList()
    if (coords.size < 2) return null
    val lat = coords[0]
    val lon = coords[1]
    val alt = if (coords.size > 2) coords[2] else null
    return VideoLocation(lat, lon, alt)
  }

  private fun getOrientation(rotation: Int?, width: Int?, height: Int?): VideoOrientation {
    if (width == null || height == null || width == 0 || height == 0) return VideoOrientation.LANDSCAPERIGHT
    val isPortrait = height > width
    val normalized = ((rotation ?: 0) % 360 + 360) % 360
    return when (normalized) {
      0 -> if (isPortrait) VideoOrientation.PORTRAIT else VideoOrientation.LANDSCAPERIGHT
      90 -> VideoOrientation.PORTRAIT
      180 -> if (isPortrait) VideoOrientation.PORTRAITUPSIDEDOWN else VideoOrientation.LANDSCAPELEFT
      270 -> VideoOrientation.PORTRAITUPSIDEDOWN
      else -> if (isPortrait) VideoOrientation.PORTRAIT else VideoOrientation.LANDSCAPERIGHT
    }
  }
}
