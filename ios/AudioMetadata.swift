//
//  AudioMetadata.swift
//  NitroVideoMetadata
//
//  Created by Antigravity on 06/02/26.
//

import Foundation
import AVFoundation

public class AudioMetadata {
  public static func getAudioInfo(from sourceURL: URL, options: VideoMetadata.Options = VideoMetadata.Options()) throws -> [String: Any] {
    let asset = AVURLAsset(url: sourceURL, options: ["AVURLAssetHTTPHeaderFieldsKey": options.headers])
    
    let semaphore = DispatchSemaphore(value: 0)
    asset.loadValuesAsynchronously(forKeys: ["tracks", "duration", "metadata"]) {
      semaphore.signal()
    }
    _ = semaphore.wait(timeout: .now() + 10)

    var error: NSError?
    guard asset.statusOfValue(forKey: "duration", error: &error) == .loaded else {
      throw error ?? NSError(domain: "AudioMetadata", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to load audio duration"])
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
    var artist = ""
    var title = ""
    var album = ""

    if let audioTrack = asset.tracks(withMediaType: .audio).first {
        bitrate = audioTrack.estimatedDataRate
        if let formatDesc = audioTrack.formatDescriptions.first as? CMAudioFormatDescription {
            if let asbd = CMAudioFormatDescriptionGetStreamBasicDescription(formatDesc)?.pointee {
                sampleRate = asbd.mSampleRate
                channels = Int(asbd.mChannelsPerFrame)
            }
            let codecType = CMFormatDescriptionGetMediaSubType(formatDesc)
            codec = fourCharCodeToString(fourCharCode: codecType)
        }
    }

    // Extract metadata (Artist, Title, Album)
    for item in asset.metadata {
        guard let key = item.commonKey?.rawValue, let value = item.value else { continue }
        switch key {
        case AVMetadataKey.commonKeyArtist.rawValue:
            artist = value as? String ?? ""
        case AVMetadataKey.commonKeyTitle.rawValue:
            title = value as? String ?? ""
        case AVMetadataKey.commonKeyAlbumName.rawValue:
            album = value as? String ?? ""
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
      "artist": artist,
      "title": title,
      "album": album
    ]
  }

  private static func fourCharCodeToString(fourCharCode: FourCharCode) -> String {
    let chars: [Character] = [
      Character(UnicodeScalar((fourCharCode >> 24) & 0xFF)!),
      Character(UnicodeScalar((fourCharCode >> 16) & 0xFF)!),
      Character(UnicodeScalar((fourCharCode >> 8) & 0xFF)!),
      Character(UnicodeScalar(fourCharCode & 0xFF)!)
    ]
    return String(chars)
  }
}
