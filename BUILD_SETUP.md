# Build Setup Instructions

## Prerequisites

1. **Java Development Kit**: Install OpenJDK 17 or 21
2. **Android SDK**: Install Android SDK (usually via Android Studio)
3. **Environment Setup**: Configure Java and Android SDK paths

## Configuration

### Option 1: Environment File (.env) - Recommended

1. **Create .env file**: Copy `.env.example` to `.env` and configure:
   ```
   JAVA_HOME_JDK21="C:\Program Files\OpenJDK\jdk-21"
   ANDROID_HOME="D:\dev\Softwares\Unity\2022.3.40f1\Editor\Data\PlaybackEngines\AndroidPlayer\SDK"
   GRADLE_JVM_ARGS="-Xmx2048m -Dfile.encoding=UTF-8"
   ```

2. **Import environment variables**:
   
   **Linux/Mac (Bash/Zsh):**
   ```bash
   set -a && source .env && set +a
   ```
   
   **Windows CMD:**
   ```cmd
   for /f "tokens=1,2 delims==" %i in (.env) do set %i=%j
   ```
   
   **Windows PowerShell:**
   ```powershell
   Get-Content .env | ForEach-Object {
       if ($_ -match '^([^=]+)=(.*)$') {
           [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim('"'), 'Process')
       }
   }
   ```

### Option 2: System Environment Variables

Set these system-wide environment variables:
- `JAVA_HOME` pointing to JDK 17+
- `ANDROID_HOME` pointing to your Android SDK (not Unity's embedded SDK)

## Build Commands

### MediaProjectionLib (Android Library)

**Linux/Mac:**
```bash
set -a && source .env && set +a && cd MediaProjectionLib && ./gradlew build
```

**Windows CMD:**
```cmd
for /f "tokens=1,2 delims==" %i in (.env) do set %i=%j
cd MediaProjectionLib
gradlew.bat build
```

**Windows PowerShell:**
```powershell
Get-Content .env | ForEach-Object { if ($_ -match '^([^=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim('"'), 'Process') } }
cd MediaProjectionLib
.\gradlew.bat build
```

### TestMediaProjectionApp (Standalone Test App)

**Linux/Mac:**
```bash
set -a && source .env && set +a && cd TestMediaProjectionApp && ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Windows CMD:**
```cmd
for /f "tokens=1,2 delims==" %i in (.env) do set %i=%j
cd TestMediaProjectionApp
gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

**Windows PowerShell:**
```powershell
Get-Content .env | ForEach-Object { if ($_ -match '^([^=]+)=(.*)$') { [Environment]::SetEnvironmentVariable($matches[1], $matches[2].Trim('"'), 'Process') } }
cd TestMediaProjectionApp
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Unity Project (QuestMediaProjection)
Use Unity Editor menu: **MediaProjection → Build Quest 3 APK**

## Troubleshooting

- **Build fails with Java errors**: Ensure Java 17+ is installed and in PATH
- **Android SDK not found**: Set `ANDROID_SDK_ROOT` environment variable
- **TestMediaProjectionApp uses wrong SDK**: Ensure `ANDROID_SDK_ROOT` points to your Android SDK, not Unity's embedded SDK
- **JDK version mismatch**: TestMediaProjectionApp requires JDK 17, MediaProjectionLib works with JDK 21
- **Gradle daemon issues**: Run `./gradlew --stop` and retry

## Notes

- All machine-specific paths have been removed from gradle.properties files
- Unity project configurations are managed by Unity Editor
- The project uses environment variables for flexible configuration across machines