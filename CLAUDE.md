# Unity Media Processing Project

## Overview
Hardware-accelerated screen recording for Unity Android using MediaProjection API and MediaCodec. Zero-copy pipeline outputs MP4 files via hardware codecs.

## Architecture

### Core Principle: Native Service + Zero-Copy Pipeline
- **Android Service**: All recording runs in background service, survives app backgrounding
- **Zero-Copy Data Flow**: `MediaProjection → VirtualDisplay → MediaCodec Surface → Hardware Encoder → MP4`
- **Unity Control Only**: C# sends start/stop commands via Intents, no frame data crosses JNI
- **Android 14+ Compatible**: Foreground service with mediaProjection type

## Project Structure
```
UnityMediaProcessing/
├── MediaProjectionLib/          # Android library (consumer-agnostic)
├── QuestMediaProjection/        # Unity project with VR integration  
├── TestMediaProjectionApp/      # Standalone Android test app
└── CLAUDE.md                   # This file
```

## Build Commands
```bash
# Build Android Library
cd MediaProjectionLib && ./gradlew build

# Build Unity APK (uses automated timestamping)
cd QuestMediaProjection && [Unity Menu: MediaProjection → Build Quest 3 APK]

# Test on Device
cd TestMediaProjectionApp && ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Status ✅ COMPLETE
- **MediaProjectionLib**: Consumer-agnostic with full MediaCodec API exposure
- **Unity Integration**: C# interfaces, services, and Quest VR specialization
- **Test Infrastructure**: Comprehensive testing with 27+ Unity tests, 11+ Android tests
- **Quest Testing**: Working on Quest 3 with frame rate automation scripts
- **Architecture**: Fully refactored - generic library, specialized consumers