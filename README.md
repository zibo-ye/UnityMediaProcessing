# Unity Media Processing - MediaProjection + MediaCodec Video Recording

A high-performance Unity Android plugin implementing hardware-accelerated screen recording using the MediaProjection API and MediaCodec with a zero-copy pipeline architecture.

## 🎯 Project Overview

This project implements a sophisticated video recording system for Unity Android applications that achieves maximum performance by delegating the entire recording workload to native Android components. The architecture ensures video frame data never enters the Unity C# environment, utilizing a direct GPU-to-encoder pipeline for optimal performance.

## 🏗️ Architecture

### Core Principle: Zero-Copy Pipeline
The implementation follows this high-performance data flow:

```
MediaProjection → VirtualDisplay → MediaCodec Input Surface → Hardware Encoder → Encoded Buffer → MediaMuxer → MP4 File
```

### Key Architectural Decisions

1. **Native Android Service**: All recording operations run in a background Android Service, independent of Unity's Activity lifecycle
2. **Surface-to-Surface Connection**: Direct connection between VirtualDisplay output and MediaCodec input surface
3. **Hardware Acceleration**: Utilizes GPU and hardware video encoders for maximum performance
4. **Android 14+ Compatibility**: Implements proper foreground service with mediaProjection type

## 📁 Project Structure

```
UnityMediaProcessing/
├── MediaProjectionLib/          # Android library (forked from t-34400/MediaProjectionLib)
│   └── app/src/main/java/com/t34400/mediaprojectionlib/
│       ├── core/                # Original MediaProjection implementation
│       └── recording/           # New video recording components
│           ├── VideoRecordingManager.kt    # Core recording pipeline
│           └── VideoRecordingService.kt    # Foreground service
├── TestMediaProjectionApp/      # Dedicated Android test application
│   └── app/src/main/java/com/test/mediaprojectionapp/
│       ├── MainActivity.kt      # Test UI with frame rate selection
│       └── res/layout/          # UI layouts with spinners for all options
├── QuestMediaProjection/        # Unity project (forked from t-34400/QuestMediaProjection)
│   └── Assets/MediaProjection/Scripts/
│       ├── Interfaces/          # Service interfaces
│       ├── Services/            # Service implementations
│       └── ViewModels/          # UI ViewModels
├── test_frame_rates.py         # Python UI automation script
├── test_frame_rates.sh         # Bash UI automation script
└── README.md                   # This file
```

## 🚀 Features

### ✅ Implemented Features (Current Status: 2025-01-27)

- **Hardware-Accelerated Recording**: Direct GPU-to-encoder pipeline with MediaCodec
- **Zero-Copy Performance**: No frame data passes through Unity C# layer
- **Variable Frame Rate Support**: Configurable frame rates (30,36,60,72,80,90 FPS)
- **Android 14+ Compatible**: Proper foreground service implementation
- **Background Recording**: Continues recording when app is backgrounded
- **Multiple Quality Presets**: Default, High Quality, Performance, and VR optimized
- **Quest 3 Testing**: Successfully deployed and tested on Quest 3 device
- **TestMediaProjectionApp**: Dedicated test application with UI automation
- **Custom Configuration**: Full control over bitrate, frame rate, and output settings
- **Real-time Notifications**: User-visible recording status and controls
- **Error Handling**: Comprehensive error reporting and recovery
- **Unity Integration**: Clean C# API with UnityEvent callbacks

### 🔄 Current Architecture Refactoring (IN PROGRESS)

**Goal**: Make MediaProjectionLib completely consumer-agnostic and expose comprehensive configuration options.

**Issues Being Addressed**:
- Remove VR-specific assumptions from MediaProjectionLib (resolution presets, frame rate presets)
- Expose all MediaCodec configuration options in RecordingConfig
- Provide capability discovery APIs for consumers
- Move application-specific presets to consumer applications (TestMediaProjectionApp, QuestMediaProjection)

### 🎮 Unity C# API

```csharp
// Get the video recording service
var recordingService = serviceContainer.VideoRecordingService;

// Start recording with default settings
recordingService.StartRecording();

// Start with custom configuration
var config = new VideoRecordingConfig
{
    videoBitrate = 8000000,    // 8 Mbps
    videoFrameRate = 60,       // 60 fps
    outputDirectory = "/custom/path"
};
recordingService.StartRecording(config);

// Stop recording
recordingService.StopRecording();

// Event handling
recordingService.OnRecordingComplete += (outputPath) => {
    Debug.Log($"Recording saved to: {outputPath}");
};
```

### 🎛️ Recording Presets

| Preset | Bitrate | Frame Rate | Use Case |
|--------|---------|------------|----------|
| **Default** | 5 Mbps | 30 fps | Balanced quality/performance |
| **High Quality** | 10 Mbps | 60 fps | Maximum quality recordings |
| **Performance** | 2 Mbps | 30 fps | Longer recordings, lower storage |
| **Custom** | User-defined | User-defined | Full control |

## 🔧 Implementation Details

### Android Components

#### VideoRecordingManager
- **Zero-copy pipeline implementation**
- MediaCodec hardware encoder setup with H.264
- VirtualDisplay with direct surface connection
- Background thread processing for optimal performance
- MediaMuxer integration for MP4 output
- Comprehensive error handling and resource cleanup

#### VideoRecordingService
- Android 14+ compatible foreground service
- MediaProjection foreground service type declaration
- Notification management with recording status
- Intent-based communication with Unity
- Proper lifecycle management and resource cleanup

### Unity Components

#### IVideoRecordingService Interface
```csharp
public interface IVideoRecordingService : IDisposable
{
    RecordingState CurrentState { get; }
    bool IsRecording { get; }
    string? CurrentOutputFile { get; }
    
    event Action<RecordingState>? OnRecordingStateChanged;
    event Action<string>? OnRecordingComplete;
    event Action<string>? OnRecordingError;
    
    bool StartRecording(VideoRecordingConfig config);
    bool StopRecording();
    RecordingStatus GetRecordingStatus();
}
```

#### VideoRecordingViewModel
- UI-friendly ViewModel with UnityEvent integration
- Recording preset management
- Progress tracking and status display
- Inspector-configurable settings

## 🛠️ Setup and Installation

### Prerequisites

- **Unity 2022.3+** (tested with 2022.3.40f1)
- **Android SDK API 29+** (MediaProjection minimum requirement)
- **Target Android 14+** (API 34) for foreground service compatibility
- **Java/JDK 11+** for building Android components

### Environment Setup

1. **Android SDK**: Using Unity's bundled SDK
   ```
   ANDROID_HOME=D:\dev\Softwares\Unity\2022.3.40f1\Editor\Data\PlaybackEngines\AndroidPlayer\SDK
   ```

2. **Java**: Using Unity's bundled OpenJDK
   ```
   JAVA_HOME=D:\dev\Softwares\Unity\2022.3.40f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK
   ```

### Building the Android Library

1. **Accept Android SDK licenses** (required for first build):
   ```bash
   %ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat --licenses
   ```

2. **Build MediaProjectionLib**:
   ```bash
   cd MediaProjectionLib
   gradlew.bat build
   ```

3. **Integration**: The built AAR will be automatically included in the Unity project

### Unity Project Setup

1. **Open QuestMediaProjection** in Unity 2022.3+
2. **Configure Android settings**:
   - Minimum API Level: 29
   - Target API Level: 34
   - Scripting Backend: IL2CPP
   - Architecture: ARM64

3. **Add VideoRecordingViewModel** to your scene
4. **Configure ServiceContainer** with `enableVideoRecording = true`

## 📱 Android Manifest Configuration

The following permissions and services are automatically configured:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

<service
  android:name="com.t34400.mediaprojectionlib.recording.VideoRecordingService"
  android:foregroundServiceType="mediaProjection"
  android:exported="false"
  android:stopWithTask="false" />
```

## 🔄 Data Flow Architecture

### Control Flow
```
Unity C# Script → JNI Bridge → Android Plugin (AAR) → Intent → Android Service
```

### Data Flow (Zero-Copy)
```
MediaProjection → VirtualDisplay → MediaCodec Input Surface → 
Hardware Encoder → Encoded Buffer → MediaMuxer → MP4 File
```

### Unity Integration
```
VideoRecordingViewModel → VideoRecordingService → JNI Bridge → 
VideoRecordingService (Android) → VideoRecordingManager → Native Pipeline
```

## 🎯 Performance Characteristics

- **Frame Rate**: Up to 60 fps depending on device capabilities
- **Latency**: ~16-33ms typical (1-2 frame delay)
- **Memory Usage**: Minimal Unity heap impact due to zero-copy design
- **CPU Usage**: Low due to hardware acceleration
- **Storage**: Efficient H.264 compression with configurable bitrates

## 🧪 Testing and Validation

### ✅ **Completed Testing Infrastructure**

#### **Unity Testing Suite (27+ Tests)**
- **Edit Mode Tests**: 15+ configuration and interface validation tests
- **Play Mode Tests**: 12+ runtime behavior and lifecycle tests  
- **Test Controller**: Manual UI + automated test sequences with real-time feedback
- **Performance Tests**: Memory leak detection and timing validation

#### **Android Unit Tests (11+ Tests)**
- **Configuration Tests**: Data class validation and integrity
- **State Management**: Enum testing and transitions
- **Performance Tests**: Object creation and memory usage
- **All Tests Pass**: ✅ `BUILD SUCCESSFUL` with clean architecture

#### **Professional Debug System**
- **Multi-Level Logging**: Verbose/Debug/Info/Warning/Error with filtering
- **Performance Tracking**: Automatic operation timing with statistics
- **Android Integration**: Direct logcat output for device debugging
- **Memory Monitoring**: GC and Unity memory tracking with leak detection
- **Log Export**: Professional session reports with system information

#### **Build Automation**
- **One-Click Builds**: Menu-driven APK generation for Quest 3
- **Build Validation**: Automatic SDK, permission, and architecture checks
- **Target Configuration**: Optimized settings for Quest 3 VR platform
- **Unity Menu Integration**: `MediaProjection → Build Settings/Testing/Documentation`

### 🎯 **Test Scenarios Ready for Device Testing**
1. **Basic Recording**: Automated lifecycle test (5-15 second recordings)
2. **Quality Presets**: Default/HighQuality/Performance/Custom configurations
3. **Background Recording**: App backgrounding during recording
4. **Error Handling**: Permission denial, invalid configs, service failures
5. **Performance Validation**: Zero-copy pipeline verification
6. **Memory Monitoring**: Leak detection during extended recording
7. **Long Duration**: Extended recording sessions (>30 minutes)

### 📱 **Device Testing Workflow**

#### **TestMediaProjectionApp (Dedicated Test App)**
```bash
# Build and install TestMediaProjectionApp
cd TestMediaProjectionApp
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Launch and test frame rates
adb shell am start -n com.test.mediaprojectionapp/.MainActivity

# Automated frame rate testing
python test_frame_rates.py
# or
bash test_frame_rates.sh
```

**TestMediaProjectionApp Features**:
- **Frame Rate Selection**: Dropdown with 30,36,60,72,80,90 FPS options
- **Codec Testing**: Query and test all available hardware codecs  
- **Resolution Options**: Multiple recording resolutions for testing
- **Bitrate Configuration**: Various bitrate presets for quality testing
- **Real Recording**: Actually creates MP4 files for validation
- **UI Automation**: Scripts for automated testing of all configurations

#### **Unity Quest 3 Testing**  
```bash
# Quest 3 Testing
adb devices
# Use Unity menu: MediaProjection → Build Quest 3 APK  
adb install -r VideoRecording_Quest3_[timestamp].apk
adb logcat | grep "Unity\|VideoRecording"
```

### ⏳ **Current Todos (Architecture Refactoring)**

**High Priority**:
- [ ] **Refactor MediaProjectionLib RecordingConfig**: Expose all MediaCodec options comprehensively
- [ ] **Remove VR Assumptions**: Move VR-specific presets from MediaProjectionLib to consumers  
- [ ] **Capability Discovery APIs**: Implement getAvailableCodecs(), getDisplayInfo(), etc.
- [ ] **Generic Helper Methods**: Replace VR-specific methods with device-agnostic ones

**Medium Priority**:
- [ ] **Update TestMediaProjectionApp**: Use new architecture with consumer-defined presets
- [ ] **Update QuestMediaProjection**: Implement VR-specific presets on Unity side
- [ ] **Rebuild and Test**: Validate refactored architecture on Quest 3
- [ ] **Documentation Update**: Update API documentation for new architecture

**Completed Recently**:
- [x] **Quest 3 Deployment**: TestMediaProjectionApp successfully installed and working
- [x] **Frame Rate Implementation**: Variable frame rate support (30,36,60,72,80,90 FPS)
- [x] **UI Automation**: Test scripts created for automated frame rate validation
- [x] **Architecture Analysis**: Identified need for consumer-agnostic design

### ✅ **Known Limitations**

#### **Quest 3 Hardware Limitations**
- **Multi-Layer Frame Rate Constraints**: Quest 3 has cascading frame rate limitations at multiple levels:
  - **MediaCodec Level**: Correctly configured (e.g., 72 FPS) ✅
  - **VirtualDisplay Level**: Limited to 60 Hz regardless of MediaCodec settings ⚠️
  - **Hardware Encoder Level**: Final MP4 output capped at ~30 FPS (avg_frame_rate ≈ 29.97) ❌
- **Root Cause**: VirtualDisplay API only exposes single 60 Hz mode, and hardware encoder further reduces output to 30 FPS
- **Diagnostic Evidence**: 
  ```
  MediaFormat KEY_FRAME_RATE: 72        ← Correctly configured
  VirtualDisplay Refresh Rate: 60.0 Hz  ← Hardware limitation
  MP4 avg_frame_rate: 29.97 FPS         ← Final output limitation
  ```
- **Workaround**: For Quest 3 users, expect 30 FPS video output regardless of frame rate selection

#### **Implementation Limitations**
- **Audio Recording**: Not yet implemented (video only)
- **Pause/Resume**: Not implemented (stop/start required)  
- **Real-time Preview**: Current implementation optimized for file output only

## 🚧 Future Enhancements

### Planned Features
- [ ] Audio recording integration with MediaRecorder
- [ ] Pause/resume functionality
- [ ] Real-time streaming capabilities
- [ ] Multiple output format support (WebM, etc.)
- [ ] Advanced encoder settings (B-frames, etc.)
- [ ] Adaptive bitrate based on device performance

### Performance Optimizations
- [ ] Dynamic resolution scaling
- [ ] Frame rate adaptation
- [ ] Battery usage optimization
- [ ] Thermal throttling management

## 📚 Repository Information

- **Original Projects**: Based on t-34400's MediaProjection API implementations
- **MediaProjectionLib**: Forked from [t-34400/MediaProjectionLib](https://github.com/t-34400/MediaProjectionLib)
- **QuestMediaProjection**: Forked from [t-34400/QuestMediaProjection](https://github.com/t-34400/QuestMediaProjection)
- **Android 14+ Reference**: Implementation inspired by [JGeraldoLima/android-media-projection-sample](https://github.com/JGeraldoLima/android-media-projection-sample) for proper MediaProjection handling on Android 14+
- **Goal**: Extend practice implementations into a full MediaProjection + MediaCodec suite

## 🤝 Contributing

This project extends the excellent foundation provided by t-34400's MediaProjection implementations. Contributions are welcome for:

- Performance optimizations
- Additional codec support
- Audio recording integration
- Enhanced error handling
- Documentation improvements

## 📄 License

This project builds upon the original work by t-34400. Please refer to the original repositories for licensing information.

## 🙏 Acknowledgments

- **t-34400** for the original MediaProjection API implementations
- **Google** for MediaProjection and MediaCodec APIs
- **Unity Technologies** for the Android development tools
- **Meta** for Quest/VR platform support

---

**Note**: This implementation prioritizes performance and Android platform compliance. The zero-copy architecture ensures minimal impact on your Unity application's performance while providing professional-quality video recording capabilities.