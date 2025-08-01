# Unity Media Processing - MediaProjection + MediaCodec Video Recording

A high-performance Unity Android plugin implementing hardware-accelerated screen recording using MediaProjection API and MediaCodec with zero-copy pipeline architecture.

## 🎯 Overview

Hardware-accelerated screen recording for Unity Android applications. Achieves maximum performance by delegating recording to native Android components with direct GPU-to-encoder pipeline.

**Core Architecture**: `MediaProjection → VirtualDisplay → MediaCodec Surface → Hardware Encoder → MP4`

## 🏗️ Key Features

- ✅ **Zero-Copy Pipeline**: No frame data crosses Unity C# layer
- ✅ **Background Recording**: Continues when app is backgrounded  
- ✅ **Hardware Acceleration**: Direct GPU-to-encoder with MediaCodec
- ✅ **Android 14+ Compatible**: Proper foreground service implementation
- ✅ **Consumer-Agnostic Architecture**: Generic library, specialized consumers
- ✅ **Quest VR Integration**: Variable frame rates with OVR display sync
- ✅ **Comprehensive Testing**: 27+ Unity tests, 11+ Android tests

## 📁 Project Structure

```
UnityMediaProcessing/
├── MediaProjectionLib/          # Android library (consumer-agnostic)
├── QuestMediaProjection/        # Unity project with VR integration  
├── TestMediaProjectionApp/      # Standalone Android test app
└── README.md                   # This file
```

## 🚀 Quick Start

### Prerequisites
- Unity 2022.3+
- Android SDK API 29+ (target API 34)
- Java/JDK 11+

### Build Commands

```bash
# Build Android Library
cd MediaProjectionLib && ./gradlew build

# Build Unity APK
# Use Unity Menu: MediaProjection → Build Quest 3 APK

# Test on Device  
cd TestMediaProjectionApp && ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Unity C# API

```csharp
// Get service and start recording
var recordingService = serviceContainer.VideoRecordingService;
recordingService.StartRecording();

// Custom configuration
var config = new VideoRecordingConfig {
    videoBitrate = 8000000,    // 8 Mbps
    videoFrameRate = 60,       // 60 fps
    outputDirectory = "/custom/path"
};
recordingService.StartRecording(config);

// Event handling
recordingService.OnRecordingComplete += (outputPath) => {
    Debug.Log($"Recording saved: {outputPath}");
};
```

## 🎛️ Recording Presets

| Preset | Bitrate | Frame Rate | Use Case |
|--------|---------|------------|----------|
| **Default** | 5 Mbps | 30 fps | Balanced quality/performance |
| **High Quality** | 10 Mbps | 60 fps | Maximum quality |
| **Performance** | 2 Mbps | 30 fps | Longer recordings |
| **Custom** | User-defined | User-defined | Full control |

## 🧪 Testing

### Unity Testing (27+ Tests)
```bash
# Run from Unity Test Runner
Window → General → Test Runner
```

### Android Testing (11+ Tests)  
```bash
cd MediaProjectionLib
./gradlew test
```

### Quest 3 Device Testing
```bash
# Automated frame rate testing
python test_frame_rates.py
```

## ⚡ Performance

- **Frame Rate**: Up to 60 fps (Quest 3: ~30 fps due to hardware limits)
- **Latency**: ~16-33ms (1-2 frame delay)
- **Memory**: Minimal Unity heap impact
- **CPU**: Low due to hardware acceleration

## 🛠️ Implementation Details

### Android Components
- **VideoRecordingManager**: Zero-copy pipeline with MediaCodec H.264
- **VideoRecordingService**: Android 14+ foreground service
- **RecordingConfig**: Comprehensive MediaCodec API exposure

### Unity Components  
- **IVideoRecordingService**: Main recording interface
- **VideoRecordingViewModel**: UI-friendly ViewModel with UnityEvents
- **ServiceContainer**: Dependency injection for all services

## 🚧 Known Limitations

- **Quest 3**: Output limited to ~30 FPS due to VirtualDisplay hardware constraints
- **Audio**: Not yet implemented (video only)
- **Pause/Resume**: Not implemented (stop/start required)

## 📚 Repository Information

- **Original**: Based on t-34400's MediaProjection implementations
- **MediaProjectionLib**: Forked from [t-34400/MediaProjectionLib](https://github.com/t-34400/MediaProjectionLib) 
- **QuestMediaProjection**: Forked from [t-34400/QuestMediaProjection](https://github.com/t-34400/QuestMediaProjection)

## 📄 License

MIT License - see [LICENSE](LICENSE) file for details.

---

**Status**: ✅ **COMPLETE** - Production-ready with comprehensive testing and Quest 3 validation.