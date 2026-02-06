package com.margelo.nitro.nitromediametadata

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.webkit.URLUtil
import androidx.core.net.toUri
import java.io.File
import java.io.InputStream
import java.net.URL

data class ImageMetadata(
    val width: Int,
    val height: Int,
    val fileSize: Long?,
    val format: String,
    val orientation: String,
    val exif: Map<String, String>,
    val location: VideoLocation?
)

class ImageMetadataReader(private val context: Context) {
    fun getImageInfo(source: String): ImageMetadata? {
        val uri = source.toUri()
        var inputStream: InputStream? = null
        try {
            inputStream = when {
                URLUtil.isFileUrl(source) -> File(uri.path!!).inputStream()
                URLUtil.isContentUrl(source) -> context.contentResolver.openInputStream(uri)
                else -> URL(source).openStream()
            }

            if (inputStream == null) return null

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            
            // Re-open stream for ExifInterface because decodeStream consumes it
            inputStream.close()
            inputStream = when {
                URLUtil.isFileUrl(source) -> File(uri.path!!).inputStream()
                URLUtil.isContentUrl(source) -> context.contentResolver.openInputStream(uri)
                else -> URL(source).openStream()
            }
            
            if (inputStream == null) return null

            val exif = ExifInterface(inputStream)
            val width = options.outWidth
            val height = options.outHeight
            val format = options.outMimeType?.substringAfter("image/") ?: ""
            
            val orientationInt = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val orientation = getOrientationString(orientationInt)
            
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
        } finally {
            inputStream?.close()
        }
    }

    private fun getOrientationString(orientation: Int): String {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> "LandscapeRight"
            ExifInterface.ORIENTATION_ROTATE_180 -> "PortraitUpsideDown"
            ExifInterface.ORIENTATION_ROTATE_270 -> "LandscapeLeft"
            else -> "Portrait"
        }
    }
}
