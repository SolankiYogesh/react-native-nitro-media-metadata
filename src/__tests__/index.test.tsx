jest.mock('react-native-nitro-modules', () => {
  const mockGetVideoInfoAsync = jest.fn();
  const mockGetAudioInfoAsync = jest.fn();
  const mockGetImageInfoAsync = jest.fn();
  const mockGetVideoThumbnailAsync = jest.fn();

  const mockHybridObject = {
    getVideoInfoAsync: mockGetVideoInfoAsync,
    getAudioInfoAsync: mockGetAudioInfoAsync,
    getImageInfoAsync: mockGetImageInfoAsync,
    getVideoThumbnailAsync: mockGetVideoThumbnailAsync,
  };

  return {
    __esModule: true,
    NitroModules: {
      createHybridObject: jest.fn(() => mockHybridObject),
    },
    __mocks: {
      mockGetVideoInfoAsync,
      mockGetAudioInfoAsync,
      mockGetImageInfoAsync,
      mockGetVideoThumbnailAsync,
    },
  };
});

import {
  getAudioInfoAsync,
  getImageInfoAsync,
  getVideoInfoAsync,
  getVideoThumbnailAsync,
} from '../index';

const {
  mockGetVideoInfoAsync,
  mockGetAudioInfoAsync,
  mockGetImageInfoAsync,
  mockGetVideoThumbnailAsync,
} = require('react-native-nitro-modules').__mocks;

beforeEach(() => {
  mockGetVideoInfoAsync.mockReset();
  mockGetAudioInfoAsync.mockReset();
  mockGetImageInfoAsync.mockReset();
  mockGetVideoThumbnailAsync.mockReset();
});

describe('getVideoInfoAsync', () => {
  it('forwards the source and defaults options to {} when omitted', async () => {
    mockGetVideoInfoAsync.mockResolvedValue({ duration: 1.5 });

    const result = await getVideoInfoAsync('file:///video.mp4');

    expect(mockGetVideoInfoAsync).toHaveBeenCalledWith('file:///video.mp4', {});
    expect(result).toEqual({ duration: 1.5 });
  });

  it('forwards explicit options unchanged', async () => {
    mockGetVideoInfoAsync.mockResolvedValue({ duration: 2 });
    const headers = { Authorization: 'Bearer token' };

    await getVideoInfoAsync('https://example.com/video.mp4', { headers });

    expect(mockGetVideoInfoAsync).toHaveBeenCalledWith(
      'https://example.com/video.mp4',
      { headers }
    );
  });

  it('propagates rejections from the native module', async () => {
    mockGetVideoInfoAsync.mockRejectedValue(new Error('native failure'));

    await expect(getVideoInfoAsync('file:///bad.mp4')).rejects.toThrow(
      'native failure'
    );
  });
});

describe('getAudioInfoAsync', () => {
  it('forwards the source and defaults options to {} when omitted', async () => {
    mockGetAudioInfoAsync.mockResolvedValue({
      duration: 10,
      artist: undefined,
    });

    const result = await getAudioInfoAsync('file:///song.mp3');

    expect(mockGetAudioInfoAsync).toHaveBeenCalledWith('file:///song.mp3', {});
    expect(result.artist).toBeUndefined();
  });

  it('forwards explicit options unchanged', async () => {
    mockGetAudioInfoAsync.mockResolvedValue({ duration: 10 });
    const headers = { 'X-Test': '1' };

    await getAudioInfoAsync('https://example.com/song.mp3', { headers });

    expect(mockGetAudioInfoAsync).toHaveBeenCalledWith(
      'https://example.com/song.mp3',
      { headers }
    );
  });
});

describe('getImageInfoAsync', () => {
  it('forwards the source and defaults options to {} when omitted', async () => {
    mockGetImageInfoAsync.mockResolvedValue({
      width: 100,
      height: 200,
      orientation: 'Portrait',
    });

    const result = await getImageInfoAsync('file:///photo.jpg');

    expect(mockGetImageInfoAsync).toHaveBeenCalledWith('file:///photo.jpg', {});
    expect(result.orientation).toBe('Portrait');
  });

  it('forwards explicit options unchanged', async () => {
    mockGetImageInfoAsync.mockResolvedValue({ width: 1, height: 1 });
    const headers = { 'X-Test': '1' };

    await getImageInfoAsync('ph://asset-id', { headers });

    expect(mockGetImageInfoAsync).toHaveBeenCalledWith('ph://asset-id', {
      headers,
    });
  });
});

describe('getVideoThumbnailAsync', () => {
  it('forwards the source and defaults options to {} when omitted', async () => {
    mockGetVideoThumbnailAsync.mockResolvedValue('data:image/jpeg;base64,abcd');

    const result = await getVideoThumbnailAsync('file:///video.mp4');

    expect(mockGetVideoThumbnailAsync).toHaveBeenCalledWith(
      'file:///video.mp4',
      {}
    );
    expect(result).toBe('data:image/jpeg;base64,abcd');
  });

  it('forwards timeMs and headers unchanged', async () => {
    mockGetVideoThumbnailAsync.mockResolvedValue(null);

    const result = await getVideoThumbnailAsync('file:///video.mp4', {
      timeMs: 5000,
      headers: { Authorization: 'Bearer token' },
    });

    expect(mockGetVideoThumbnailAsync).toHaveBeenCalledWith(
      'file:///video.mp4',
      {
        timeMs: 5000,
        headers: { Authorization: 'Bearer token' },
      }
    );
    expect(result).toBeNull();
  });
});
