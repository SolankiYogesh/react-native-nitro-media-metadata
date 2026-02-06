import type { HybridObject } from 'react-native-nitro-modules';

export type VideoLocationType = {
  latitude: number;
  longitude: number;
  altitude?: number;
};

export type VideoInfoResult = {
  /**
   * Duration of the video in seconds (float).
   */
  duration: number;
  /**
   * Tells if the video has a audio track. If the video has no audio track, its considered a mute video.
   */
  hasAudio: boolean;
  /**
   * Available only on iOS >= 14 and Android. Tells if the video is a HDR video.
   * Will return null if it could not be determined. (e.g. on older iOS/Android versions)
   */
  isHDR: boolean | null;
  /**
   * Width of the video in pixels.
   */
  width: number;
  /**
   * Height of the video in pixels.
   */
  height: number;
  /**
   * Frame rate of the video in frames per second.
   * Works on iOS and Android (except Safari).
   */
  fps: number;
  /**
   * Bit rate of the video in bits per second.
   * Supported on all platforms.
   */
  bitRate: number;
  /**
   * File size of the video in bytes. Works only for local files, returns 0 for remote files.
   * Supported on all platforms.
   */
  fileSize: number;
  /**
   * Video codec.
   * Supported on all platforms
   */
  codec: string;
  /**
   * Video orientation.
   * Supported on all platforms
   * Orientation takes into account both the natural dimensions AND any rotation/transform applied to the video:
   * - Portrait: The video is in portrait mode.
   * - PortraitUpsideDown: The video is in portrait mode, but upside down.
   * - Landscape: The video is in landscape mode.
   * - LandscapeRight: The video is in landscape mode, but rotated 90 degrees clockwise.
   * - LandscapeLeft: The video is in landscape mode, but rotated 90 degrees counter-clockwise.
   */
  orientation: string;
  /**
   * Natural orientation of the video.
   * This is the orientation of the video as it was recorded, without any rotation/transform applied.
   */
  naturalOrientation: string;
  /**
   * Aspect ratio of the video.
   */
  aspectRatio: number;
  /**
   * Tells if the video is 16:9.
   */
  is16_9: boolean;
  /**
   * Audio sample rate of the video in samples per second.
   */
  audioSampleRate: number;
  /**
   * Audio channel count of the video.
   */
  audioChannels: number;
  /**
   * Audio codec of the video.
   */
  audioCodec: string;
  /**
   * Location where the video was recorded.
   * Supported on iOS and Android (if the video contains location metadata)
   */
  location: VideoLocationType | null;
};

export type AudioInfoResult = {
  /**
   * Duration of the audio in seconds (float).
   */
  duration: number;
  /**
   * File size of the audio in bytes. Works only for local files, returns 0 for remote files.
   */
  fileSize: number;
  /**
   * Audio codec.
   */
  audioCodec: string;
  /**
   * Audio sample rate in samples per second.
   */
  sampleRate: number;
  /**
   * Audio channel count.
   */
  channels: number;
  /**
   * Bit rate in bits per second.
   */
  bitRate: number;
  /**
   * Artist name (if available).
   */
  artist?: string;
  /**
   * Title (if available).
   */
  title?: string;
  /**
   * Album name (if available).
   */
  album?: string;
};

export type ImageInfoResult = {
  /**
   * Width of the image in pixels.
   */
  width: number;
  /**
   * Height of the image in pixels.
   */
  height: number;
  /**
   * File size of the image in bytes. Works only for local files, returns 0 for remote files.
   */
  fileSize: number;
  /**
   * Image format (e.g. jpeg, png).
   */
  format: string;
  /**
   * Image orientation.
   */
  orientation: string;
  /**
   * EXIF data (if available).
   */
  exif?: Record<string, string>;
  /**
   * Location where the image was taken (if available).
   */
  location?: VideoLocationType | null;
};

export type MediaInfoOptions = {
  /**
   * In case `sourceFilename` is a remote URI, `headers` object is passed in a network request.
   */
  headers?: Record<string, string>;
};

export interface NitroMediaMetadata extends HybridObject<{
  ios: 'swift';
  android: 'kotlin';
}> {
  getVideoInfoAsync(
    source: string,
    options: MediaInfoOptions
  ): Promise<VideoInfoResult>;
  getAudioInfoAsync(
    source: string,
    options: MediaInfoOptions
  ): Promise<AudioInfoResult>;
  getImageInfoAsync(
    source: string,
    options: MediaInfoOptions
  ): Promise<ImageInfoResult>;
}
