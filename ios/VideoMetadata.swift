//
//  VideoMetadata.swift
//  NitroMediaMetadata
//
//  Created by Yogesh Solanki on 29/10/25.
//

import Foundation
import AVFoundation
import CoreMedia

public class VideoMetadata {
  
  public struct Options {
    public var headers: [String: String] = [:]
    public init(headers: [String: String] = [:]) {
      self.headers = headers
    }
  }

  public static func getVideoInfo(from sourceURL: URL, options: Options = Options()) throws -> [String: Any] {
    var assetOptions: [String: Any] = [:]
    if !sourceURL.isFileURL && sourceURL.scheme != "ph" {
      assetOptions["AVURLAssetHTTPHeaderFieldsKey"] = options.headers
    }
    let asset = AVURLAsset(url: sourceURL, options: assetOptions)
    
    let semaphore = DispatchSemaphore(value: 0)
    asset.loadValuesAsynchronously(forKeys: ["tracks", "duration"]) {
      semaphore.signal()
    }
    _ = semaphore.wait(timeout: .now() + 10)

    var error: NSError?
    let tracksStatus = asset.statusOfValue(forKey: "tracks", error: &error)
    if tracksStatus != .loaded {
      throw error ?? NSError(domain: "VideoMetadata", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to load video tracks (Status: \(tracksStatus.rawValue))"])
    }

    let duration = CMTimeGetSeconds(asset.duration)
    let hasAudio = !asset.tracks(withMediaType: .audio).isEmpty

    var fileSize: Int64 = 0
    if sourceURL.isFileURL,
       let fileAttributes = try? FileManager.default.attributesOfItem(atPath: sourceURL.path),
       let size = fileAttributes[.size] as? NSNumber {
      fileSize = size.int64Value
    }

    var bitrate: Float = 0
    var width: Int = 0
    var height: Int = 0
    var frameRate: Float = 0
    var codec = ""
    var orientation = ""
    var isHDR = false
    var audioSampleRate = 0
    var audioChannels = 0
    var audioCodec = ""
    var location: [String: Double]? = nil

    if let videoTrack = asset.tracks(withMediaType: .video).first {
      bitrate = videoTrack.estimatedDataRate
      let size = videoTrack.naturalSize.applying(videoTrack.preferredTransform)
      width = Int(abs(size.width))
      height = Int(abs(size.height))
      frameRate = videoTrack.nominalFrameRate
      orientation = getOrientation(from: videoTrack)
      
      if let formatDesc = videoTrack.formatDescriptions.first {
          let description = formatDesc as! CMFormatDescription
          let codecType = CMFormatDescriptionGetMediaSubType(description)
          codec = fourCharCodeToString(fourCharCode: codecType)
      }

      if #available(iOS 14.0, *) {
        isHDR = videoTrack.hasMediaCharacteristic(.containsHDRVideo)
      }
    }

    if let audioTrack = asset.tracks(withMediaType: .audio).first,
       let firstFormatDescription = audioTrack.formatDescriptions.first {
      let description = firstFormatDescription as! CMFormatDescription
      if let audioStreamBasicDescription = CMAudioFormatDescriptionGetStreamBasicDescription(description)?.pointee {
        audioSampleRate = Int(audioStreamBasicDescription.mSampleRate)
        audioChannels = Int(audioStreamBasicDescription.mChannelsPerFrame)
      }
      let codecType = CMFormatDescriptionGetMediaSubType(description)
      audioCodec = fourCharCodeToString(fourCharCode: codecType)
    }

    if let gpsData = extractGPSData(from: asset.metadata) {
      location = gpsData
    }

    let aspectRatio = height > 0 ? Double(width) / Double(height) : 0
    let is16_9 = height > 0 && fabs((Double(width) / Double(height)) - (16.0 / 9.0)) < 0.01

    return [
      "duration": duration,
      "hasAudio": hasAudio,
      "isHDR": isHDR,
      "fileSize": fileSize,
      "bitrate": bitrate,
      "fps": frameRate,
      "width": width,
      "height": height,
      "codec": codec,
      "orientation": orientation,
      "naturalOrientation": height > width ? "Portrait" : "Landscape",
      "aspectRatio": aspectRatio,
      "is16_9": is16_9,
      "audioSampleRate": audioSampleRate,
      "audioChannels": audioChannels,
      "audioCodec": audioCodec,
      "location": location as Any
    ]
  }

  private static func getOrientation(from videoTrack: AVAssetTrack) -> String {
    let transform = videoTrack.preferredTransform
    let angle = atan2(transform.b, transform.a)
    let degrees = Int(round(angle * 180 / .pi)) % 360
    switch degrees {
      case 0: return "LandscapeRight"
      case 90, -270: return "Portrait"
      case 180, -180: return "LandscapeLeft"
      case 270, -90: return "PortraitUpsideDown"
      default: return "Unknown"
    }
  }

  private static func extractGPSData(from metadata: [AVMetadataItem]) -> [String: Double]? {
    if let locationItem = metadata.first(where: { ($0.key as? String) == "com.apple.quicktime.location.ISO6709" }),
       let locationString = locationItem.stringValue {
      return parseISO6709(locationString)
    }
    return nil
  }

  private static func parseISO6709(_ string: String) -> [String: Double]? {
    let cleaned = string.replacingOccurrences(of: "/", with: "")
    let regex = try! NSRegularExpression(pattern: "([+-]\\d+\\.\\d+)")
    let matches = regex.matches(in: cleaned, range: NSRange(cleaned.startIndex..., in: cleaned))
    let coords = matches.compactMap {
      Double((cleaned as NSString).substring(with: $0.range))
    }
    guard coords.count >= 2 else { return nil }
    var result: [String: Double] = ["latitude": coords[0], "longitude": coords[1]]
    if coords.count > 2 { result["altitude"] = coords[2] }
    return result
  }
  
  private static func fourCharCodeToString(fourCharCode: FourCharCode) -> String {
    let bytes: [UInt8] = [
      UInt8((fourCharCode >> 24) & 0xFF),
      UInt8((fourCharCode >> 16) & 0xFF),
      UInt8((fourCharCode >> 8) & 0xFF),
      UInt8(fourCharCode & 0xFF)
    ]
    
    return bytes.compactMap { byte in
      let c = Character(UnicodeScalar(byte))
      return c.isASCII ? String(c) : nil
    }.joined().trimmingCharacters(in: .whitespaces)
  }
}
