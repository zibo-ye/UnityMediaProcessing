#!/bin/bash

# UI Automation Test Script for TestMediaProjectionApp Frame Rate Testing
# Tests frame rate functionality using ADB commands

APP_PACKAGE="com.test.mediaprojectionapp"
ACTIVITY="${APP_PACKAGE}/.MainActivity"

echo "🚀 Starting TestMediaProjectionApp Frame Rate Testing"
echo "=========================================="

# Function to run adb command with error handling
run_adb() {
    echo "🔧 Running: adb $*"
    adb "$@"
    sleep 1
}

# Function to tap at coordinates
tap() {
    local x=$1
    local y=$2
    echo "👆 Tapping at ($x, $y)"
    run_adb shell input tap $x $y
}

# Function to test a specific frame rate
test_frame_rate() {
    local frame_rate_name=$1
    local spinner_index=$2
    
    echo ""
    echo "🧪 Testing Frame Rate: $frame_rate_name"
    echo "-----------------------------"
    
    # Tap frame rate spinner (estimated coordinates for 1080p screen)
    echo "📱 Opening frame rate spinner..."
    tap 540 1200
    
    # Calculate position for frame rate option (each option ~100px apart)
    local option_y=$((400 + spinner_index * 100))
    echo "🎯 Selecting $frame_rate_name..."
    tap 540 $option_y
    
    # Brief recording test
    echo "🎬 Starting recording test..."
    tap 540 900  # Start Recording button
    
    # Handle MediaProjection permission if it appears
    sleep 2
    echo "✅ Handling permission dialog..."
    tap 800 1000  # Start now / Allow button
    
    # Record for 3 seconds
    echo "⏺️ Recording for 3 seconds..."
    sleep 3
    
    # Stop recording
    echo "⏹️ Stopping recording..."
    tap 540 900  # Stop Recording button
    
    sleep 2
    echo "✅ Frame rate test completed: $frame_rate_name"
}

# Main test execution
main() {
    echo "🔐 Granting permissions..."
    run_adb shell pm grant $APP_PACKAGE android.permission.RECORD_AUDIO
    run_adb shell pm grant $APP_PACKAGE android.permission.WRITE_EXTERNAL_STORAGE
    run_adb shell pm grant $APP_PACKAGE android.permission.READ_EXTERNAL_STORAGE
    run_adb shell pm grant $APP_PACKAGE android.permission.POST_NOTIFICATIONS
    
    echo "🚀 Launching app..."
    run_adb shell am start -n $ACTIVITY
    sleep 3
    
    echo "📊 Querying available codecs first..."
    tap 540 800  # Query Available Codecs button
    sleep 2
    
    # Test each frame rate preset
    declare -a frame_rates=(
        "30 FPS (Standard)"
        "36 FPS (Cinema+)" 
        "60 FPS (Smooth)"
        "72 FPS (VR Standard)"
        "80 FPS (High Performance)"
        "90 FPS (VR Premium)"
    )
    
    for i in "${!frame_rates[@]}"; do
        test_frame_rate "${frame_rates[$i]}" $i
        sleep 1
    done
    
    echo ""
    echo "📊 Frame Rate Testing Complete!"
    echo "=============================="
    
    # Check for recorded files
    echo "🔍 Checking for recorded files..."
    run_adb shell find /sdcard -name "recording_*.mp4" -type f 2>/dev/null || echo "Checking alternative storage..."
    run_adb shell find /storage/emulated/0 -name "recording_*.mp4" -type f 2>/dev/null || echo "No recordings found in standard locations"
    
    echo "✅ Test sequence completed!"
}

# Check if device is connected
if ! adb devices | grep -q "device$"; then
    echo "❌ No Android device connected"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi

# Run main test
main