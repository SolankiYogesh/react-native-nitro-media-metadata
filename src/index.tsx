import { NitroModules } from 'react-native-nitro-modules';
import type {
  AudioInfoResult,
  ImageInfoResult,
  MediaInfoOptions,
  NitroMediaMetadata,
  VideoInfoResult,
  VideoThumbnailOptions,
} from './NitroMediaMetadata.nitro';

const NitroMediaMetadataHybridObject =
  NitroModules.createHybridObject<NitroMediaMetadata>('NitroMediaMetadata');

export function getVideoInfoAsync(
  source: string,
  options?: MediaInfoOptions
): Promise<VideoInfoResult> {
  return NitroMediaMetadataHybridObject.getVideoInfoAsync(
    source,
    options ?? {}
  );
}

export function getAudioInfoAsync(
  source: string,
  options?: MediaInfoOptions
): Promise<AudioInfoResult> {
  return NitroMediaMetadataHybridObject.getAudioInfoAsync(
    source,
    options ?? {}
  );
}

export function getImageInfoAsync(
  source: string,
  options?: MediaInfoOptions
): Promise<ImageInfoResult> {
  return NitroMediaMetadataHybridObject.getImageInfoAsync(
    source,
    options ?? {}
  );
}

export function getVideoThumbnailAsync(
  source: string,
  options?: VideoThumbnailOptions
): Promise<string | null> {
  return NitroMediaMetadataHybridObject.getVideoThumbnailAsync(
    source,
    options ?? {}
  );
}

export type {
  AudioInfoResult,
  ImageInfoResult,
  ImageOrientation,
  MediaInfoOptions,
  NaturalOrientation,
  VideoInfoResult,
  VideoLocationType,
  VideoOrientation,
  VideoThumbnailOptions,
} from './NitroMediaMetadata.nitro';
