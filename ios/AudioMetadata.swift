//
//  AudioMetadata.swift
//  NitroMediaMetadata
//
//  Created by Yogesh on 06/02/26.
//

import Foundation
import AVFoundation

public class AudioMetadata {
  public static func getAudioInfo(from sourceURL: URL, options: VideoMetadata.Options = VideoMetadata.Options()) throws -> [String: Any] {
    var assetOptions: [String: Any] = [:]
    if !sourceURL.isFileURL && sourceURL.scheme != "ph" {
      assetOptions["AVURLAssetHTTPHeaderFieldsKey"] = options.headers
    }
    let asset = AVURLAsset(url: sourceURL, options: assetOptions)
    
    let semaphore = DispatchSemaphore(value: 0)
    asset.loadValuesAsynchronously(forKeys: ["tracks", "duration", "metadata"]) {
      semaphore.signal()
    }
    _ = semaphore.wait(timeout: .now() + 10)

    var error: NSError?
    let durationStatus = asset.statusOfValue(forKey: "duration", error: &error)
    if durationStatus != .loaded {
      throw error ?? NSError(domain: "AudioMetadata", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to load audio duration (Status: \(durationStatus.rawValue))"])
    }

    let duration = CMTimeGetSeconds(asset.duration)
    
    var fileSize: Int64 = 0
    if sourceURL.isFileURL,
       let fileAttributes = try? FileManager.default.attributesOfItem(atPath: sourceURL.path),
       let size = fileAttributes[.size] as? NSNumber {
      fileSize = size.int64Value
    }

    var bitrate: Float = 0
    var sampleRate: Double = 0
    var channels: Int = 0
    var codec = ""
    // Missing artist/title/album/coverArt are left `nil` (not ""), matching Android's
    // `undefined`/null for these genuinely-optional fields.
    var artist: String? = nil
    var title: String? = nil
    var album: String? = nil
    var coverArt: String? = nil

    if let audioTrack = asset.tracks(withMediaType: .audio).first {
        bitrate = audioTrack.estimatedDataRate
        if let formatDesc = audioTrack.formatDescriptions.first {
            let description = formatDesc as! CMFormatDescription
            if let asbd = CMAudioFormatDescriptionGetStreamBasicDescription(description)?.pointee {
                sampleRate = asbd.mSampleRate
                channels = Int(asbd.mChannelsPerFrame)
            }
            let codecType = CMFormatDescriptionGetMediaSubType(description)
            codec = MediaMetadataUtils.fourCharCodeToString(fourCharCode: codecType)
        }
    }

    // Extract metadata (Artist, Title, Album, embedded cover art)
    for item in asset.metadata {
        guard let key = item.commonKey?.rawValue else { continue }
        switch key {
        case AVMetadataKey.commonKeyArtist.rawValue:
            if let str = item.value as? String, !str.isEmpty { artist = str }
        case AVMetadataKey.commonKeyTitle.rawValue:
            if let str = item.value as? String, !str.isEmpty { title = str }
        case AVMetadataKey.commonKeyAlbumName.rawValue:
            if let str = item.value as? String, !str.isEmpty { album = str }
        case AVMetadataKey.commonKeyArtwork.rawValue:
            if let data = item.dataValue, !data.isEmpty {
                coverArt = MediaMetadataUtils.dataURI(from: data)
            }
        default:
            break
        }
    }

    return [
      "duration": duration,
      "fileSize": fileSize,
      "codec": codec,
      "sampleRate": sampleRate,
      "channels": channels,
      "bitRate": bitrate,
      "artist": artist as Any,
      "title": title as Any,
      "album": album as Any,
      "coverArt": coverArt as Any
    ]
  }
}
