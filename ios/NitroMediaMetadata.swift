import Foundation
import AVFoundation
import Photos
import NitroModules

struct MediaMetadataError: LocalizedError {
  let message: String
  var errorDescription: String? { message }
}

func doubleValue(_ value: Any?) -> Double {
  if let num = value as? NSNumber { return num.doubleValue }
  if let dbl = value as? Double { return dbl }
  if let flt = value as? Float { return Double(flt) }
  if let intVal = value as? Int { return Double(intVal) }
  return 0
}

class NitroMediaMetadata: HybridNitroMediaMetadataSpec {

  func getVideoInfoAsync(source: String, options: MediaInfoOptions) throws -> NitroModules.Promise<VideoInfoResult> {
    let promise = NitroModules.Promise<VideoInfoResult>()
    let url = resolveURL(source)

    guard let url = url else {
      promise.reject(withError: MediaMetadataError(message: "Invalid URL: \(source)"))
      return promise
    }
    
    resolveMediaURL(url) { resolvedURL in
      guard let videoURL = resolvedURL else {
        promise.reject(withError: MediaMetadataError(message: "Failed to resolve video URI."))
        return
      }

      DispatchQueue.global(qos: .userInitiated).async {
        do {
          let infoDict = try VideoMetadata.getVideoInfo(from: videoURL, options: VideoMetadata.Options(headers: options.headers ?? [:]))
          let result = self.mapToVideoInfoResult(infoDict)
          promise.resolve(withResult: result)
        } catch {
          promise.reject(withError: MediaMetadataError(message: "Failed to extract video info: \(error.localizedDescription)"))
        }
      }
    }
    
    return promise
  }

  func getAudioInfoAsync(source: String, options: MediaInfoOptions) throws -> Promise<AudioInfoResult> {
    let promise = Promise<AudioInfoResult>()
    let url = resolveURL(source)

    guard let url = url else {
      promise.reject(withError: MediaMetadataError(message: "Invalid URL: \(source)"))
      return promise
    }

    resolveMediaURL(url) { resolvedURL in
      guard let audioURL = resolvedURL else {
        promise.reject(withError: MediaMetadataError(message: "Failed to resolve audio URI."))
        return
      }

      DispatchQueue.global(qos: .userInitiated).async {
        do {
          let infoDict = try AudioMetadata.getAudioInfo(from: audioURL, options: VideoMetadata.Options(headers: options.headers ?? [:]))
          let result = self.mapToAudioInfoResult(infoDict)
          promise.resolve(withResult: result)
        } catch {
          promise.reject(withError: MediaMetadataError(message: "Failed to extract audio info: \(error.localizedDescription)"))
        }
      }
    }
    
    return promise
  }

  func getImageInfoAsync(source: String, options: MediaInfoOptions) throws -> Promise<ImageInfoResult> {
    let promise = Promise<ImageInfoResult>()
    let url = resolveURL(source)

    guard let url = url else {
      promise.reject(withError: MediaMetadataError(message: "Invalid URL: \(source)"))
      return promise
    }

    resolveMediaURL(url) { resolvedURL in
      guard let imageURL = resolvedURL else {
        promise.reject(withError: MediaMetadataError(message: "Failed to resolve image URI."))
        return
      }

      DispatchQueue.global(qos: .userInitiated).async {
        do {
          let infoDict = try ImageMetadata.getImageInfo(from: imageURL)
          let result = self.mapToImageInfoResult(infoDict)
          promise.resolve(withResult: result)
        } catch {
          promise.reject(withError: MediaMetadataError(message: "Failed to extract image info: \(error.localizedDescription)"))
        }
      }
    }
    
    return promise
  }

  private func resolveURL(_ source: String) -> URL? {
    if let url = URL(string: source), url.scheme != nil {
      return url
    }
    
    if source.starts(with: "/") {
      return URL(fileURLWithPath: source)
    }
    
    if let encoded = source.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed),
       let url = URL(string: encoded), url.scheme != nil {
      return url
    }
    
    return nil
  }

  private func mapToVideoInfoResult(_ infoDict: [String: Any]) -> VideoInfoResult {
    return VideoInfoResult(
      duration: doubleValue(infoDict["duration"]),
      hasAudio: infoDict["hasAudio"] as? Bool ?? false,
      isHDR: infoDict["isHDR"] as? Bool ?? false,
      width: doubleValue(infoDict["width"]),
      height: doubleValue(infoDict["height"]),
      fps: doubleValue(infoDict["fps"]),
      bitRate: doubleValue(infoDict["bitrate"]),
      fileSize: doubleValue(infoDict["fileSize"]),
      codec: infoDict["codec"] as? String ?? "",
      orientation: infoDict["orientation"] as? String ?? "",
      naturalOrientation: infoDict["naturalOrientation"] as? String ?? "",
      aspectRatio: doubleValue(infoDict["aspectRatio"]),
      is16_9: infoDict["is16_9"] as? Bool ?? false,
      audioSampleRate: doubleValue(infoDict["audioSampleRate"]),
      audioChannels: doubleValue(infoDict["audioChannels"]),
      audioCodec: infoDict["audioCodec"] as? String ?? "",
      location: {
        if let loc = infoDict["location"] as? [String: Double] {
          return VideoLocationType(
            latitude: loc["latitude"] ?? 0,
            longitude: loc["longitude"] ?? 0,
            altitude: loc["altitude"]
          )
        }
        return nil
      }()
    )
  }

  private func mapToAudioInfoResult(_ infoDict: [String: Any]) -> AudioInfoResult {
    return AudioInfoResult(
      duration: doubleValue(infoDict["duration"]),
      fileSize: doubleValue(infoDict["fileSize"]),
      audioCodec: infoDict["codec"] as? String ?? "",
      sampleRate: doubleValue(infoDict["sampleRate"]),
      channels: doubleValue(infoDict["channels"]),
      bitRate: doubleValue(infoDict["bitRate"]),
      artist: infoDict["artist"] as? String,
      title: infoDict["title"] as? String,
      album: infoDict["album"] as? String
    )
  }

  private func mapToImageInfoResult(_ infoDict: [String: Any]) -> ImageInfoResult {
    return ImageInfoResult(
      width: doubleValue(infoDict["width"]),
      height: doubleValue(infoDict["height"]),
      fileSize: doubleValue(infoDict["fileSize"]),
      format: infoDict["format"] as? String ?? "",
      orientation: infoDict["orientation"] as? String ?? "",
      exif: infoDict["exif"] as? [String: String],
      location: {
        if let loc = infoDict["location"] as? [String: Double] {
          return VideoLocationType(
            latitude: loc["latitude"] ?? 0,
            longitude: loc["longitude"] ?? 0,
            altitude: loc["altitude"]
          )
        }
        return nil
      }()
    )
  }

  private func resolveMediaURL(_ uri: URL, completion: @escaping (URL?) -> Void) {
    if uri.scheme == "ph" {
      let assetID = uri.absoluteString.replacingOccurrences(of: "ph://", with: "")
      let results = PHAsset.fetchAssets(withLocalIdentifiers: [assetID], options: nil)
      guard let phAsset = results.firstObject else {
        completion(nil)
        return
      }

      if phAsset.mediaType == .image {
        let options = PHContentEditingInputRequestOptions()
        options.isNetworkAccessAllowed = true
        phAsset.requestContentEditingInput(with: options) { input, _ in
          completion(input?.fullSizeImageURL)
        }
      } else {
        let options = PHVideoRequestOptions()
        options.isNetworkAccessAllowed = true

        PHImageManager.default().requestAVAsset(forVideo: phAsset, options: options) { avAsset, _, _ in
          if let urlAsset = avAsset as? AVURLAsset {
            completion(urlAsset.url)
          } else {
            completion(nil)
          }
        }
      }
    } else {
      completion(uri)
    }
  }
}
