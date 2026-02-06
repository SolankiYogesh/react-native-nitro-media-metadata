package com.margelo.nitro.nitromediametadata

import com.margelo.nitro.NitroModules
import com.margelo.nitro.core.Promise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NitroMediaMetadata: HybridNitroMediaMetadataSpec() {

  override fun getVideoInfoAsync(
    source: String,
    options: MediaInfoOptions
  ): Promise<VideoInfoResult> {
    val promise = Promise<VideoInfoResult>()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        NitroModules.applicationContext?.let{ctx->
          val meta = VideoMetadataReader(ctx).getVideoInfo(source, options.headers)
          if (meta == null) {
            CoroutineScope(Dispatchers.Main).launch {
              promise.reject(Exception("Failed to read video metadata"))
            }
            return@launch
          }

          val result = VideoInfoResult(
            duration = meta.duration,
            hasAudio = meta.hasAudio,
            isHDR = meta.isHDR,
            width = (meta.width ?: 0).toDouble(),
            height = (meta.height ?: 0).toDouble(),
            fps = (meta.fps ?: 0f).toDouble(),
            bitRate = (meta.bitrate ?: 0).toDouble(),
            fileSize = (meta.fileSize ?: 0L).toDouble(),
            codec = meta.videoCodec ?: "",
            orientation = meta.orientation,
            naturalOrientation = meta.naturalOrientation,
            aspectRatio = meta.aspectRatio ?: 0.0,
            is16_9 = meta.is16_9,
            audioSampleRate = (meta.audioSampleRate ?: 0).toDouble(),
            audioChannels = (meta.audioChannels ?: 0).toDouble(),
            audioCodec = meta.audioCodec ?: "",
            location = meta.location?.let {
              VideoLocationType(
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude
              )
            }
          )

          CoroutineScope(Dispatchers.Main).launch {
            promise.resolve(result)
          }
        }


      } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
          promise.reject(e)
        }
      }
    }

    return promise
  }

  override fun getAudioInfoAsync(
    source: String,
    options: MediaInfoOptions
  ): Promise<AudioInfoResult> {
    val promise = Promise<AudioInfoResult>()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        NitroModules.applicationContext?.let { ctx ->
          val meta = AudioMetadataReader(ctx).getAudioInfo(source, options.headers)
          if (meta == null) {
            CoroutineScope(Dispatchers.Main).launch {
              promise.reject(Exception("Failed to read audio metadata"))
            }
            return@launch
          }

          val result = AudioInfoResult(
            duration = meta.duration,
            fileSize = (meta.fileSize ?: 0L).toDouble(),
            audioCodec = meta.codec ?: "",
            sampleRate = (meta.sampleRate ?: 0).toDouble(),
            channels = (meta.channels ?: 0).toDouble(),
            bitRate = (meta.bitRate ?: 0).toDouble(),
            artist = meta.artist,
            title = meta.title,
            album = meta.album
          )

          CoroutineScope(Dispatchers.Main).launch {
            promise.resolve(result)
          }
        }
      } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
          promise.reject(e)
        }
      }
    }

    return promise
  }

  override fun getImageInfoAsync(
    source: String,
    options: MediaInfoOptions
  ): Promise<ImageInfoResult> {
    val promise = Promise<ImageInfoResult>()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        NitroModules.applicationContext?.let { ctx ->
          val meta = ImageMetadataReader(ctx).getImageInfo(source)
          if (meta == null) {
            CoroutineScope(Dispatchers.Main).launch {
              promise.reject(Exception("Failed to read image metadata"))
            }
            return@launch
          }

          val result = ImageInfoResult(
            width = meta.width.toDouble(),
            height = meta.height.toDouble(),
            fileSize = (meta.fileSize ?: 0L).toDouble(),
            format = meta.format,
            orientation = meta.orientation,
            exif = meta.exif,
            location = meta.location?.let {
              VideoLocationType(
                latitude = it.latitude,
                longitude = it.longitude,
                altitude = it.altitude
              )
            }
          )

          CoroutineScope(Dispatchers.Main).launch {
            promise.resolve(result)
          }
        }
      } catch (e: Exception) {
        CoroutineScope(Dispatchers.Main).launch {
          promise.reject(e)
        }
      }
    }

    return promise
  }
}
