import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  Button,
  StyleSheet,
  ScrollView,
  Alert,
} from 'react-native';
import { launchImageLibrary } from 'react-native-image-picker';
import { getVideoInfoAsync } from 'react-native-nitro-media-metadata';

export default function App() {
  const [info, setInfo] = useState<any>(null);
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);

  const handlePickVideo = async () => {
    setInfo(null);
    setLoading(true);

    try {
      const result = await launchImageLibrary({
        mediaType: 'video',
        includeExtra: true,
      });

      if (result.didCancel || !result.assets?.[0]) {
        setLoading(false);
        return;
      }

      const video = result.assets[0];
      console.log('Picked video:', video.uri);

      const res = await getVideoInfoAsync(video.uri!, { headers: {} });
      console.log('Video Info:', res);
      setInfo(res);
    } catch (error: any) {
      console.error('Error:', error);
      Alert.alert('Error', error.message || 'Failed to get video info');
    } finally {
      setLoading(false);
    }
  };

  const handleFetchFromUrl = async () => {
    if (!url.trim()) {
      Alert.alert('Please enter a video URL');
      return;
    }

    setInfo(null);
    setLoading(true);

    try {
      console.log('Fetching metadata for URL:', url);
      const res = await getVideoInfoAsync(url.trim(), { headers: {} });
      console.log('Video Info:', res);
      setInfo(res);
    } catch (error: any) {
      console.error('Error:', error);
      Alert.alert('Error', error.message || 'Failed to get video info');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.section}>
        <Button title="Pick Video from Device" onPress={handlePickVideo} />
      </View>

      <View style={styles.section}>
        <TextInput
          value={url}
          onChangeText={setUrl}
          placeholder="Enter video URL (e.g. https://example.com/video.mp4)"
          style={styles.input}
          autoCapitalize="none"
        />
        <Button title="Fetch from URL" onPress={handleFetchFromUrl} />
      </View>

      {loading && <Text style={styles.loading}>⏳ Loading...</Text>}

      {info && (
        <ScrollView style={styles.resultBox}>
          <Text style={styles.heading}>Video Info:</Text>
          {Object.entries(info).map(([key, value]) => (
            <Text key={key} style={styles.text}>
              {key}: {JSON.stringify(value)}
            </Text>
          ))}
        </ScrollView>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    paddingTop: 60,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    textAlign: 'center',
    marginBottom: 24,
  },
  section: {
    marginBottom: 24,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 8,
    padding: 10,
    marginBottom: 8,
  },
  loading: {
    textAlign: 'center',
    color: '#888',
    marginVertical: 12,
  },
  heading: {
    fontWeight: 'bold',
    fontSize: 16,
    marginBottom: 8,
  },
  resultBox: {
    marginTop: 20,
    backgroundColor: '#f7f7f7',
    padding: 12,
    borderRadius: 8,
    maxHeight: 400,
  },
  text: {
    fontSize: 13,
    marginBottom: 4,
  },
});
