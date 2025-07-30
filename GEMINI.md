# Unity Media Processing Project

## Project Overview
This project implements hardware-accelerated screen recording in Unity for Android using the MediaProjection API and MediaCodec. The goal is to create a high-performance, zero-copy video recording pipeline that outputs MP4 files using hardware codecs.

## Build Commands
- **Android Library Build**: `cd MediaProjectionLib && gradlew.bat build`
- **Test Commands**: To be determined based on project structure

## Architecture

### Section 1: Architectural Blueprint for Native Screen Recording
To implement a high-performance, reliable screen recording feature within a Unity application for Android, a carefully considered architecture is paramount. A naive approach that attempts to manage media processing from within Unity's C# scripts will inevitably encounter performance bottlenecks, lifecycle issues, and platform-specific limitations. The optimal architecture delegates the entire recording workload to the native Android layer, leveraging a background Service to manage a zero-copy data pipeline. This design ensures maximum performance, stability, and adherence to modern Android platform requirements.

#### 1.1 The Core Principle: Offloading to a Native Service
The most robust and performant architecture for this task involves delegating the entire recording workload to a native Android Service. A Unity application on Android runs within an Activity, a component whose lifecycle is tied to the user's direct interaction with the app's UI. If the user backgrounds the application, the Activity can be paused or even destroyed by the operating system to conserve resources. Any recording process managed directly by the Activity would be abruptly terminated in such a scenario.

An Android Service, by contrast, is a component designed for performing long-running operations in the background, independent of the application's UI. By encapsulating the screen recording logic within a Service, the recording can persist even if the Unity Activity is paused. This decoupling of the recording lifecycle from the UI lifecycle is the cornerstone of a stable implementation.

Furthermore, modern versions of the Android OS have made this architecture a requirement, not just a best practice. As of Android 14 (API level 34), any application using the MediaProjection API must declare and start a foreground service of type mediaProjection. This is a security and user-transparency measure, ensuring the user is always aware that the screen is being captured via a persistent notification. An architecture that fails to use a foreground Service will not function on up-to-date Android devices.

#### 1.2 The High-Performance Data Flow: Bypassing Unity
A critical architectural decision for achieving high performance is to ensure that the raw video frame data never enters the Unity C# environment. The process of capturing, encoding, and muxing video is computationally intensive. Transferring multi-megabyte video frames from the native Android layer to the Unity C# managed environment across the Java Native Interface (JNI) bridge on every frame would introduce catastrophic performance overhead, leading to dropped frames, stuttering, and high CPU usage.

The optimal solution is a direct, zero-copy data pipeline that exists entirely on the native side. This is achieved by leveraging the Surface API, a fundamental component of the Android graphics stack. The MediaProjection API can create a VirtualDisplay, which mirrors the screen's content and renders it directly to a provided Surface. Concurrently, the MediaCodec API, when configured for encoding, can expose an input Surface of its own via the createInputSurface() method.

By connecting the VirtualDisplay's output to the MediaCodec's input Surface, a direct data path is established. The GPU renders the screen content, and this data is passed directly to the hardware video encoder without ever being copied to main memory or crossing the JNI boundary into Unity. The C# layer's role is relegated to that of a high-level controller, sending commands like "start" and "stop," rather than acting as a data processor.

The resulting data flow is as follows:
MediaProjection -> VirtualDisplay -> MediaCodec Input Surface -> Hardware Encoder -> Encoded Buffer -> MediaMuxer -> MP4 File.

This entire pipeline resides within the Android Service, ensuring maximum efficiency and minimal impact on the Unity application's rendering performance. Attempting to preview the recording within Unity would require a much more complex pipeline involving SurfaceTexture, GL_TEXTURE_EXTERNAL_OES, and framebuffer objects (FBOs), which is suitable for previewing but detrimental for file-only recording.

#### 1.3 The Control and Communication Flow
While the data flow is entirely native, the control flow originates from Unity. The user interacts with the Unity UI, which triggers C# methods. These methods must then communicate with the background Service on the Android side. The most effective way to achieve this is by using Android Intents.

The control path is structured as follows:
Unity C# Script -> JNI Bridge -> Plugin Facade (AAR) -> Intent -> Android Service

The roles of each component in this control chain are:

**Unity C# Script**: This is the high-level, user-facing API within the Unity project. It exposes simple methods like StartRecording() and StopRecording() and manages the UI state.

**Android Plugin (AAR)**: This is a native Android library that acts as a facade. It contains a thin layer of Java or Kotlin code that receives calls from Unity's C# scripts via the JNI bridge. Its primary responsibility is to construct and send Intents to the MediaProjectionService to initiate actions. It also manages the Activity required for the initial permission request.

**Android Service**: This is the workhorse component. It listens for incoming Intents, starts and stops the recording pipeline, manages the lifecycle of MediaProjection, MediaCodec, and MediaMuxer, and handles all the background processing.

This decoupled control flow ensures that the Unity application remains responsive, as it only sends lightweight, asynchronous messages to the native layer without waiting for the heavy lifting to complete.

## Project Structure
```
UnityMediaProcessing/
├── MediaProjectionLib/          # Android library submodule (your fork from t-34400)
├── QuestMediaProjection/        # Unity project submodule (your fork from t-34400)
└── CLAUDE.md                   # This documentation file
```

## Repository Information
- **Original Projects**: t-34400's MediaProjection API implementations
- **MediaProjectionLib**: Your fork at https://github.com/zibo-ye/MediaProjectionLib (forked from t-34400/MediaProjectionLib)
- **QuestMediaProjection**: Your fork at https://github.com/zibo-ye/QuestMediaProjection (forked from t-34400/QuestMediaProjection)
- **Goal**: Extend these practice implementations into a full MediaProjection + MediaCodec suite for Unity Android

## Development Environment
- **Unity Version**: 2022.3.40f1
- **Android SDK**: Using Unity's bundled SDK at `D:\dev\Softwares\Unity\2022.3.40f1\Editor\Data\PlaybackEngines\AndroidPlayer\SDK`
- **Java/JDK**: Using Unity's bundled OpenJDK at `D:\dev\Softwares\Unity\2022.3.40f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK`
- **Build Tools**: Gradle wrapper (gradlew.bat)

## Environment Variables
- `ANDROID_HOME`: `D:\dev\Softwares\Unity\2022.3.40f1\Editor\Data\PlaybackEngines\AndroidPlayer\SDK`
- `JAVA_HOME`: `D:\dev\Softwares\Unity\2022.3.40f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK`

## Development Workflow

### Complete Build Pipeline
```bash
# 1. Build Android Library
cd MediaProjectionLib
./gradlew build

# 2. Copy AAR to Unity
cp app/build/outputs/aar/app-release.aar ../QuestMediaProjection/Assets/Plugins/Android/libs/media-projection.aar

# 3. Build Unity APK with timestamp
cd ../QuestMediaProjection
"D:\dev\Softwares\Unity\2022.3.40f1\Editor\Unity.exe" -batchmode -quit -projectPath . -buildTarget Android -executeMethod MediaProjection.Editor.AndroidBuildConfigurator.BuildAndroidAPK -logFile ./Build/build.log
```

### Build Commands
- **Android Library Build**: `cd MediaProjectionLib && ./gradlew build`
- **Unity APK Build**: Uses `BuildAndroidAPK(string description)` method
- **Output Format**: `YYYYMMDD_HHMMSS_{description}.apk` in Build folder

### Recent Fixes
- **ImageUtils Crash Fix**: Fixed buffer overflow in `convertToBitmap` function
- **Automated AAR Integration**: Streamlined library-to-Unity workflow
- **Timestamped APK Naming**: Dynamic build naming for version tracking

### Next Steps
1. ✅ Accept Android SDK licenses
2. ✅ Build and test MediaProjectionLib  
3. ✅ Integrate with Unity project
4. ✅ Implement Unity C# interface
5. 🔄 Test hardware acceleration and performance on Quest