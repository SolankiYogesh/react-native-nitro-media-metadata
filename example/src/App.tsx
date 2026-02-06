import { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Alert,
  SafeAreaView,
  StatusBar,
  ActivityIndicator,
} from 'react-native';
import { pick, types } from '@react-native-documents/picker';
import { launchImageLibrary } from 'react-native-image-picker';
import {
  getVideoInfoAsync,
  getAudioInfoAsync,
  getImageInfoAsync,
} from 'react-native-nitro-media-metadata';

type MediaType = 'video' | 'audio' | 'image';

export default function App() {
  const [info, setInfo] = useState<any>(null);
  const [url, setUrl] = useState('');
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<MediaType>('video');

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  const formatDuration = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    const ms = Math.floor((seconds % 1) * 1000);
    return `${mins}:${secs.toString().padStart(2, '0')}.${ms
      .toString()
      .padStart(3, '0')}`;
  };

  const handlePickMedia = async () => {
    setInfo(null);
    setLoading(true);

    try {
      let uri: string | undefined;

      if (activeTab === 'audio') {
        const [result] = await pick({
          type: [types.audio],
        });
        uri = result?.uri;
      } else {
        const result = await launchImageLibrary({
          mediaType: activeTab === 'video' ? 'video' : 'photo',
          includeExtra: true,
        });

        if (result.didCancel || !result.assets?.[0]) {
          setLoading(false);
          return;
        }
        uri = result.assets[0].uri;
      }

      if (!uri) {
        setLoading(false);
        return;
      }

      let res;
      if (activeTab === 'video') {
        res = await getVideoInfoAsync(uri, { headers: {} });
      } else if (activeTab === 'image') {
        res = await getImageInfoAsync(uri, { headers: {} });
      } else if (activeTab === 'audio') {
        res = await getAudioInfoAsync(uri, { headers: {} });
      }

      setInfo(res);
    } catch (error: any) {
      console.error('Error:', error);
      Alert.alert('Error', error.message || 'Failed to get metadata');
    } finally {
      setLoading(false);
    }
  };

  const handleFetchFromUrl = async () => {
    if (!url.trim()) {
      Alert.alert('Please enter a URL');
      return;
    }

    setInfo(null);
    setLoading(true);

    try {
      let res;
      if (activeTab === 'video') {
        res = await getVideoInfoAsync(url.trim(), { headers: {} });
      } else if (activeTab === 'audio') {
        res = await getAudioInfoAsync(url.trim(), { headers: {} });
      } else if (activeTab === 'image') {
        res = await getImageInfoAsync(url.trim(), { headers: {} });
      }

      setInfo(res);
    } catch (error: any) {
      console.error('Error:', error);
      Alert.alert('Error', error.message || 'Failed to get metadata');
    } finally {
      setLoading(false);
    }
  };

  const renderInfoItem = (label: string, value: any) => {
    let displayValue = value;
    if (label === 'fileSize') displayValue = formatFileSize(value as number);
    if (label === 'duration') displayValue = formatDuration(value as number);
    if (typeof value === 'boolean') displayValue = value ? 'Yes' : 'No';
    if (value === null || value === undefined) displayValue = 'N/A';
    if (typeof value === 'object')
      displayValue = JSON.stringify(value, null, 2);

    return (
      <View key={label} style={styles.infoItem}>
        <Text style={styles.infoLabel}>{label}:</Text>
        <Text style={styles.infoValue}>{displayValue}</Text>
      </View>
    );
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />
      <View style={styles.header}>
        <Text style={styles.title}>Nitro Media Metadata</Text>
        <Text style={styles.subtitle}>
          Ultra-fast Native Metadata Extraction
        </Text>
      </View>

      <View style={styles.tabBar}>
        {(['video', 'audio', 'image'] as MediaType[]).map((tab) => (
          <TouchableOpacity
            key={tab}
            style={[styles.tab, activeTab === tab && styles.activeTab]}
            onPress={() => {
              setActiveTab(tab);
              setInfo(null);
            }}
          >
            <Text
              style={[
                styles.tabText,
                activeTab === tab && styles.activeTabText,
              ]}
            >
              {tab.toUpperCase()}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <ScrollView contentContainerStyle={styles.scrollContent}>
        <View style={styles.card}>
          <Text style={styles.cardTitle}>Source</Text>

          <TouchableOpacity
            style={styles.primaryButton}
            onPress={handlePickMedia}
          >
            <Text style={styles.buttonText}>
              Pick {activeTab} from{' '}
              {activeTab === 'audio' ? 'Files' : 'Gallery'}
            </Text>
          </TouchableOpacity>

          <View style={styles.divider}>
            <View style={styles.line} />
            <Text style={styles.dividerText}>OR</Text>
            <View style={styles.line} />
          </View>

          <TextInput
            value={url}
            onChangeText={setUrl}
            placeholder={`Enter ${activeTab} URL...`}
            style={styles.input}
            autoCapitalize="none"
            placeholderTextColor="#999"
          />
          <TouchableOpacity
            style={styles.secondaryButton}
            onPress={handleFetchFromUrl}
          >
            <Text style={styles.secondaryButtonText}>Fetch from URL</Text>
          </TouchableOpacity>
        </View>

        {loading && (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="large" color="#007AFF" />
            <Text style={styles.loadingText}>Extracting Metadata...</Text>
          </View>
        )}

        {info && (
          <View style={styles.card}>
            <View style={styles.cardHeader}>
              <Text style={styles.cardTitle}>Metadata Results</Text>
              <TouchableOpacity onPress={() => setInfo(null)}>
                <Text style={styles.clearText}>Clear</Text>
              </TouchableOpacity>
            </View>
            <View style={styles.infoList}>
              {Object.entries(info).map(([key, value]) =>
                renderInfoItem(key, value)
              )}
            </View>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F2F2F7',
  },
  header: {
    padding: 20,
    backgroundColor: '#FFF',
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#C6C6C8',
  },
  title: {
    fontSize: 28,
    fontWeight: '800',
    color: '#000',
    letterSpacing: 0.5,
  },
  subtitle: {
    fontSize: 14,
    color: '#8E8E93',
    marginTop: 4,
  },
  tabBar: {
    flexDirection: 'row',
    backgroundColor: '#FFF',
    paddingHorizontal: 20,
    paddingBottom: 10,
  },
  tab: {
    flex: 1,
    paddingVertical: 10,
    alignItems: 'center',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  activeTab: {
    borderBottomColor: '#007AFF',
  },
  tabText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#8E8E93',
  },
  activeTabText: {
    color: '#007AFF',
  },
  scrollContent: {
    padding: 16,
  },
  card: {
    backgroundColor: '#FFF',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    elevation: 3,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: '#000',
    marginBottom: 16,
  },
  primaryButton: {
    backgroundColor: '#007AFF',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
  },
  buttonText: {
    color: '#FFF',
    fontSize: 16,
    fontWeight: '600',
  },
  divider: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 16,
  },
  line: {
    flex: 1,
    height: StyleSheet.hairlineWidth,
    backgroundColor: '#C6C6C8',
  },
  dividerText: {
    marginHorizontal: 10,
    color: '#8E8E93',
    fontSize: 12,
    fontWeight: '600',
  },
  input: {
    backgroundColor: '#F2F2F7',
    borderRadius: 10,
    padding: 12,
    fontSize: 14,
    color: '#000',
    marginBottom: 12,
  },
  secondaryButton: {
    borderWidth: 1,
    borderColor: '#007AFF',
    paddingVertical: 12,
    borderRadius: 10,
    alignItems: 'center',
  },
  secondaryButtonText: {
    color: '#007AFF',
    fontSize: 15,
    fontWeight: '600',
  },
  loadingContainer: {
    marginVertical: 20,
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 10,
    color: '#8E8E93',
    fontSize: 14,
  },
  infoList: {
    marginTop: 0,
  },
  infoItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#E5E5EA',
  },
  infoLabel: {
    fontSize: 14,
    color: '#8E8E93',
    fontWeight: '500',
    flex: 1,
  },
  infoValue: {
    fontSize: 14,
    color: '#000',
    fontWeight: '600',
    flex: 2,
    textAlign: 'right',
  },
  clearText: {
    color: '#FF3B30',
    fontSize: 14,
    fontWeight: '600',
  },
});
