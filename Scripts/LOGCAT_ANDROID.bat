@echo off
for /f "tokens=1-4 delims=/:. " %%a in ('echo %time%') do (
    set timestamp=%date:~0,4%%date:~5,2%%date:~8,2%_%%a%%b%%c
)
set timestamp=%timestamp: =0%

if not exist "Logs" mkdir Logs

powershell "adb logcat -s VideoRecordingManager,VideoRecordingService,MainActivity -v time | Tee-Object -FilePath 'Logs/%timestamp%_android.log'"