import { NitroModules } from 'react-native-nitro-modules';
import type {
  NitroVideoMetadata,
  VideoInfoOptions,
  VideoInfoResult,
  AudioInfoResult,
  ImageInfoResult,
} from './NitroVideoMetadata.nitro';

const NitroVideoMetadataHybridObject =
  NitroModules.createHybridObject<NitroVideoMetadata>('NitroVideoMetadata');

export function getVideoInfoAsync(
  source: string,
  options: VideoInfoOptions
): Promise<VideoInfoResult> {
  return NitroVideoMetadataHybridObject.getVideoInfoAsync(source, options);
}

export function getAudioInfoAsync(
  source: string,
  options: VideoInfoOptions
): Promise<AudioInfoResult> {
  return NitroVideoMetadataHybridObject.getAudioInfoAsync(source, options);
}

export function getImageInfoAsync(
  source: string,
  options: VideoInfoOptions
): Promise<ImageInfoResult> {
  return NitroVideoMetadataHybridObject.getImageInfoAsync(source, options);
}
