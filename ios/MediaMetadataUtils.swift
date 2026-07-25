//
//  MediaMetadataUtils.swift
//  NitroMediaMetadata
//
//  Shared helpers used by the Audio/Video/Image metadata readers, to avoid duplicating the
//  same logic (four-char-code decoding, orientation derivation, format detection, ...) across
//  multiple files.
//

import Foundation
import ImageIO
import UniformTypeIdentifiers

public class MediaMetadataUtils {

  /// Converts a QuickTime/AVFoundation four-character-code (FourCharCode) into its
  /// human readable ASCII string representation (e.g. `avc1`, `aac `, `hvc1`).
  static func fourCharCodeToString(fourCharCode: FourCharCode) -> String {
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

  /// EXIF orientation values that imply a 90°/270° rotation. For these, the effective
  /// (as-displayed) width/height are swapped relative to the raw decoded pixel dimensions.
  private static let rotatedExifOrientations: Set<Int> = [5, 6, 7, 8]

  /// Derives a simple `Landscape` / `Portrait` / `Square` orientation string from the raw
  /// (un-rotated) decoded pixel dimensions and the EXIF orientation tag, instead of hardcoding
  /// a single value regardless of the image's actual dimensions.
  static func imageOrientationString(rawWidth: Int, rawHeight: Int, exifOrientation: Int) -> String {
    let swapsDimensions = rotatedExifOrientations.contains(exifOrientation)
    let effectiveWidth = swapsDimensions ? rawHeight : rawWidth
    let effectiveHeight = swapsDimensions ? rawWidth : rawHeight

    if effectiveWidth > effectiveHeight { return "Landscape" }
    if effectiveHeight > effectiveWidth { return "Portrait" }
    return "Square"
  }

  /// Detects the actual image format from the decoded content (as opposed to the source
  /// URL's file extension), mirroring Android's `BitmapFactory.Options.outMimeType`-based
  /// detection. Returns the MIME subtype (e.g. `jpeg`, `png`, `heic`, `webp`) or an empty
  /// string if it could not be determined.
  static func imageFormat(from imageSource: CGImageSource) -> String {
    guard let uti = CGImageSourceGetType(imageSource) as String? else { return "" }

    if let type = UTType(uti), let mime = type.preferredMIMEType {
      return mime.replacingOccurrences(of: "image/", with: "")
    }

    return ""
  }

  /// Sniffs the first few bytes of an image blob to determine its MIME type, for cases
  /// (e.g. embedded audio cover art) where no file extension/UTI is available.
  private static func sniffImageMime(bytes: Data) -> String {
    let prefix = [UInt8](bytes.prefix(12))
    if prefix.count >= 8, prefix[0] == 0x89, prefix[1] == 0x50, prefix[2] == 0x4E, prefix[3] == 0x47 {
      return "image/png"
    }
    if prefix.count >= 3, prefix[0] == 0xFF, prefix[1] == 0xD8, prefix[2] == 0xFF {
      return "image/jpeg"
    }
    if prefix.count >= 12, prefix[8] == 0x57, prefix[9] == 0x45, prefix[10] == 0x42, prefix[11] == 0x50 {
      return "image/webp"
    }
    // Embedded audio artwork is almost always JPEG or PNG; default to JPEG.
    return "image/jpeg"
  }

  /// Encodes raw image data (e.g. embedded cover art, a generated thumbnail) as a base64 data URI.
  static func dataURI(from data: Data, mimeType: String? = nil) -> String {
    let mime = mimeType ?? sniffImageMime(bytes: data)
    return "data:\(mime);base64,\(data.base64EncodedString())"
  }
}
