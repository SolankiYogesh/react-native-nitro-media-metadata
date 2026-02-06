//
//  ImageMetadata.swift
//  NitroMediaMetadata
//
//  Created by Yogesh on 06/02/26.
//

import Foundation
import ImageIO
import CoreLocation

public class ImageMetadata {
    public static func getImageInfo(from sourceURL: URL) throws -> [String: Any] {
        guard let source = CGImageSourceCreateWithURL(sourceURL as CFURL, nil) else {
            throw NSError(domain: "ImageMetadata", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to create image source"])
        }
        
        guard let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [String: Any] else {
            throw NSError(domain: "ImageMetadata", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to copy image properties"])
        }
        
        let width = properties[kCGImagePropertyPixelWidth as String] as? Int ?? 0
        let height = properties[kCGImagePropertyPixelHeight as String] as? Int ?? 0
        let orientationValue = properties[kCGImagePropertyOrientation as String] as? Int ?? 1
        let orientation = getOrientationString(from: orientationValue)
        
        var fileSize: Int64 = 0
        if sourceURL.isFileURL,
           let fileAttributes = try? FileManager.default.attributesOfItem(atPath: sourceURL.path),
           let size = fileAttributes[.size] as? NSNumber {
            fileSize = size.int64Value
        }
        
        let format = sourceURL.pathExtension.lowercased()
        
        var exif: [String: Any] = [:]
        if let exifDict = properties[kCGImagePropertyExifDictionary as String] as? [String: Any] {
            exif = exifDict
        }
        
        var location: [String: Double]? = nil
        if let gpsDict = properties[kCGImagePropertyGPSDictionary as String] as? [String: Any] {
            location = extractGPSData(from: gpsDict)
        }
        
        return [
            "width": width,
            "height": height,
            "fileSize": fileSize,
            "format": format,
            "orientation": orientation,
            "exif": exif,
            "location": location as Any
        ]
    }
    
    private static func getOrientationString(from value: Int) -> String {
        switch value {
        case 1: return "Portrait"
        case 3: return "PortraitUpsideDown"
        case 6: return "LandscapeRight"
        case 8: return "LandscapeLeft"
        default: return "Portrait"
        }
    }
    
    private static func extractGPSData(from gpsDict: [String: Any]) -> [String: Double]? {
        guard let lat = gpsDict[kCGImagePropertyGPSLatitude as String] as? Double,
              let lon = gpsDict[kCGImagePropertyGPSLongitude as String] as? Double,
              let latRef = gpsDict[kCGImagePropertyGPSLatitudeRef as String] as? String,
              let lonRef = gpsDict[kCGImagePropertyGPSLongitudeRef as String] as? String else {
            return nil
        }
        
        let latitude = (latRef == "S") ? -lat : lat
        let longitude = (lonRef == "W") ? -lon : lon
        
        var result: [String: Double] = ["latitude": latitude, "longitude": longitude]
        if let alt = gpsDict[kCGImagePropertyGPSAltitude as String] as? Double {
            result["altitude"] = alt
        }
        return result
    }
}
