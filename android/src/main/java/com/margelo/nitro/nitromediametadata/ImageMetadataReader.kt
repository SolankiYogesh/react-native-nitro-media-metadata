package com.margelo.nitro.nitromediametadata

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.webkit.URLUtil
import androidx.core.net.toUri
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URL

data class ImageMetadata(
    val width: Int,
    val height: Int,
    val fileSize: Long?,
    val format: String,
    val orientation: ImageOrientation,
    val exif: Map<String, String>,
    val location: VideoLocation?
)

class ImageMetadataReader(private val context: Context) {
    fun getImageInfo(source: String): ImageMetadata? {
        val uri = source.toUri()
        try {
            // Read the source once and reuse the in-memory buffer for both the bounds-only
            // decode and the ExifInterface read below, instead of opening the (potentially
            // remote/content://) stream twice.
            val bytes = openSourceStream(source, uri)?.use { it.readBytes() } ?: return null

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(ByteArrayInputStream(bytes), null, options)

            val exif = ExifInterface(ByteArrayInputStream(bytes))
            val width = options.outWidth
            val height = options.outHeight
            val format = options.outMimeType?.substringAfter("image/") ?: ""

            val orientationInt = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val orientation = MediaMetadataUtils.imageOrientation(width, height, orientationInt)

            val exifMap = mutableMapOf<String, String>()
            val tags = arrayOf(
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_SOFTWARE,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_ISO_SPEED_RATINGS,
                ExifInterface.TAG_SHUTTER_SPEED_VALUE,
                ExifInterface.TAG_APERTURE_VALUE,
                ExifInterface.TAG_BRIGHTNESS_VALUE
            )
            for (tag in tags) {
                exif.getAttribute(tag)?.let { exifMap[tag] = it }
            }

            val latLong = FloatArray(2)
            var location: VideoLocation? = null
            if (exif.getLatLong(latLong)) {
                val alt = exif.getAltitude(0.0)
                location = VideoLocation(latLong[0].toDouble(), latLong[1].toDouble(), alt)
            }

            var fileSize: Long? = null
            if (URLUtil.isFileUrl(source)) {
                fileSize = File(uri.path!!).length()
            }

            return ImageMetadata(
                width, height, fileSize, format, orientation, exifMap, location
            )
        } catch (e: Exception) {
            return null
        }
    }

    // NOTE: for very large remote images, buffering the entire stream into memory (as done
    // above) trades the double I/O for a larger peak memory footprint. This is an acceptable
    // trade-off for typical photo-sized images; if this library ever needs to support huge
    // remote images, a mark/reset-capable BufferedInputStream with a bounded read-ahead limit
    // would be a better fit than a full in-memory buffer.
    private fun openSourceStream(source: String, uri: Uri): InputStream? = when {
        URLUtil.isFileUrl(source) -> File(uri.path!!).inputStream()
        URLUtil.isContentUrl(source) -> context.contentResolver.openInputStream(uri)
        else -> URL(source).openStream()
    }
}
