package com.margelo.nitro.nitromediametadata

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Shared helpers used by the audio/video/image metadata readers, to avoid duplicating the
 * same logic (mime-to-codec mapping, orientation derivation, ...) across multiple readers.
 */
object MediaMetadataUtils {

  /** Maps a track MIME type (e.g. "audio/mp4a-latm", "video/avc") to a short codec name. */
  fun mapMimeTypeToCodecName(mime: String): String = when {
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

  /**
   * EXIF orientation values that imply a 90deg/270deg rotation. For these, the effective
   * (as-displayed) width/height are swapped relative to the raw decoded pixel dimensions.
   */
  private val ROTATED_EXIF_ORIENTATIONS = setOf(5, 6, 7, 8)

  /**
   * Derives a simple Landscape/Portrait/Square orientation from the raw (un-rotated) decoded
   * pixel dimensions and the EXIF orientation tag, instead of hardcoding a single value
   * regardless of the image's actual dimensions.
   */
  fun imageOrientation(rawWidth: Int, rawHeight: Int, exifOrientation: Int): ImageOrientation {
    val swapsDimensions = ROTATED_EXIF_ORIENTATIONS.contains(exifOrientation)
    val effectiveWidth = if (swapsDimensions) rawHeight else rawWidth
    val effectiveHeight = if (swapsDimensions) rawWidth else rawHeight

    return when {
      effectiveWidth > effectiveHeight -> ImageOrientation.LANDSCAPE
      effectiveHeight > effectiveWidth -> ImageOrientation.PORTRAIT
      else -> ImageOrientation.SQUARE
    }
  }

  /** Sniffs the first few bytes of an image blob to determine its MIME type. */
  private fun sniffImageMime(bytes: ByteArray): String {
    if (bytes.size >= 8 &&
      bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
      bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
    ) {
      return "image/png"
    }
    if (bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte()) {
      return "image/jpeg"
    }
    if (bytes.size >= 12 &&
      bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() &&
      bytes[10] == 'B'.code.toByte() && bytes[11] == 'P'.code.toByte()
    ) {
      return "image/webp"
    }
    // Embedded audio artwork (ID3 APIC / MP4 covr) is almost always JPEG or PNG; default to JPEG.
    return "image/jpeg"
  }

  /** Encodes a raw image byte array (e.g. embedded cover art) as a base64 data URI. */
  fun bytesToDataUri(bytes: ByteArray): String {
    val mime = sniffImageMime(bytes)
    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
    return "data:$mime;base64,$base64"
  }

  /** Encodes a decoded [Bitmap] (e.g. a video thumbnail frame) as a base64 JPEG data URI. */
  fun bitmapToJpegDataUri(bitmap: Bitmap, quality: Int = 90): String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
    val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    return "data:image/jpeg;base64,$base64"
  }
}
