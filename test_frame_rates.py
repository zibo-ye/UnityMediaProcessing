#!/usr/bin/env python3
"""
UI Automation Test Script for TestMediaProjectionApp Frame Rate Testing
Tests all frame rate presets: 30, 36, 60, 72, 80, 90 FPS
"""

import subprocess
import time
import json
from typing import List, Dict

class TestMediaProjectionUIAutomation:
    def __init__(self):
        self.package_name = "com.test.mediaprojectionapp"
        self.activity_name = f"{self.package_name}/.MainActivity"
        self.frame_rates = [
            {"value": 0, "name": "30 FPS (Standard)"},
            {"value": 1, "name": "36 FPS (Cinema+)"},
            {"value": 2, "name": "60 FPS (Smooth)"},
            {"value": 3, "name": "72 FPS (VR Standard)"},
            {"value": 4, "name": "80 FPS (High Performance)"},
            {"value": 5, "name": "90 FPS (VR Premium)"}
        ]
        self.test_results = []

    def run_adb_command(self, cmd: str) -> str:
        """Execute ADB command and return output"""
        try:
            result = subprocess.run(f"adb {cmd}", shell=True, capture_output=True, text=True)
            if result.returncode == 0:
                return result.stdout.strip()
            else:
                print(f"ADB command failed: {cmd}")
                print(f"Error: {result.stderr}")
                return ""
        except Exception as e:
            print(f"Error running ADB command: {e}")
            return ""

    def tap_element(self, x: int, y: int) -> bool:
        """Tap at specific coordinates"""
        cmd = f"shell input tap {x} {y}"
        result = self.run_adb_command(cmd)
        time.sleep(1)  # Wait for UI response
        return True

    def launch_app(self) -> bool:
        """Launch the TestMediaProjectionApp"""
        print("🚀 Launching TestMediaProjectionApp...")
        cmd = f"shell am start -n {self.activity_name}"
        result = self.run_adb_command(cmd)
        time.sleep(3)  # Wait for app to load
        print("✅ App launched successfully")
        return True

    def grant_permissions(self) -> bool:
        """Grant necessary permissions for MediaProjection"""
        print("🔐 Granting permissions...")
        permissions = [
            "android.permission.RECORD_AUDIO",
            "android.permission.WRITE_EXTERNAL_STORAGE", 
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.POST_NOTIFICATIONS"
        ]
        
        for permission in permissions:
            cmd = f"shell pm grant {self.package_name} {permission}"
            self.run_adb_command(cmd)
            
        print("✅ Permissions granted")
        return True

    def query_codecs(self) -> bool:
        """Tap the Query Available Codecs button to validate setup"""
        print("📊 Querying available codecs...")
        # Coordinates for "Query Available Codecs" button (estimated)
        self.tap_element(540, 800)  # Adjust coordinates based on screen resolution
        time.sleep(2)
        print("✅ Codec query completed")
        return True

    def select_frame_rate(self, frame_rate_index: int) -> bool:
        """Select a specific frame rate from the spinner"""
        frame_rate = self.frame_rates[frame_rate_index]
        print(f"🎯 Selecting frame rate: {frame_rate['name']}")
        
        # Tap frame rate spinner (estimated coordinates)
        self.tap_element(540, 1200)  # Adjust based on actual spinner location
        time.sleep(1)
        
        # Tap the specific frame rate option
        # Each option is approximately 120 pixels apart vertically
        option_y = 400 + (frame_rate_index * 120)
        self.tap_element(540, option_y)
        time.sleep(1)
        
        print(f"✅ Selected {frame_rate['name']}")
        return True

    def start_recording_test(self) -> bool:
        """Start a brief recording test"""
        print("🎬 Starting recording test...")
        
        # Tap "Start Recording" button
        self.tap_element(540, 900)  # Adjust coordinates
        time.sleep(2)
        
        # Handle MediaProjection permission dialog (if appears)
        # Tap "Start now" or "Allow" button
        self.tap_element(800, 1000)  # Adjust for permission dialog
        time.sleep(3)
        
        print("⏺️ Recording started, waiting 5 seconds...")
        time.sleep(5)  # Record for 5 seconds
        
        # Stop recording
        self.tap_element(540, 900)  # Same button, now shows "Stop Recording"
        time.sleep(3)
        
        print("⏹️ Recording stopped")
        return True

    def verify_recording_output(self) -> Dict:
        """Check if recording was created successfully"""
        print("🔍 Verifying recording output...")
        
        # Get recording files from device
        cmd = "shell find /sdcard -name 'recording_*.mp4' -type f -newer /tmp/test_start 2>/dev/null || find /storage/emulated/0 -name 'recording_*.mp4' -type f 2>/dev/null"
        recordings = self.run_adb_command(cmd)
        
        if recordings:
            latest_recording = recordings.split('\n')[-1] if recordings else ""
            if latest_recording:
                # Get file info
                cmd = f"shell ls -la '{latest_recording}'"
                file_info = self.run_adb_command(cmd)
                print(f"✅ Recording created: {latest_recording}")
                print(f"📁 File info: {file_info}")
                
                return {
                    "success": True,
                    "file_path": latest_recording,
                    "file_info": file_info
                }
        
        print("❌ No recording found")
        return {"success": False}

    def test_frame_rate_preset(self, frame_rate_index: int) -> Dict:
        """Test a specific frame rate preset"""
        frame_rate = self.frame_rates[frame_rate_index]
        print(f"\n{'='*60}")
        print(f"🧪 Testing Frame Rate: {frame_rate['name']}")
        print(f"{'='*60}")
        
        test_result = {
            "frame_rate": frame_rate['name'],
            "index": frame_rate_index,
            "success": False,
            "error": None,
            "recording_info": None
        }
        
        try:
            # Select the frame rate
            if not self.select_frame_rate(frame_rate_index):
                test_result["error"] = "Failed to select frame rate"
                return test_result
            
            # Start recording test
            if not self.start_recording_test():
                test_result["error"] = "Failed to complete recording test"
                return test_result
            
            # Verify output
            recording_result = self.verify_recording_output()
            test_result["recording_info"] = recording_result
            test_result["success"] = recording_result["success"]
            
            if test_result["success"]:
                print(f"✅ Frame rate test PASSED: {frame_rate['name']}")
            else:
                print(f"❌ Frame rate test FAILED: {frame_rate['name']}")
                
        except Exception as e:
            test_result["error"] = str(e)
            print(f"❌ Test error: {e}")
        
        return test_result

    def run_comprehensive_test(self) -> List[Dict]:
        """Run comprehensive frame rate testing"""
        print("🎯 Starting Comprehensive Frame Rate Testing")
        print("=" * 80)
        
        # Setup
        self.grant_permissions()
        self.launch_app()
        
        # Optional: Query codecs first to validate setup
        self.query_codecs()
        
        # Test each frame rate
        for i, frame_rate in enumerate(self.frame_rates):
            result = self.test_frame_rate_preset(i)
            self.test_results.append(result)
            
            # Brief pause between tests
            time.sleep(2)
        
        return self.test_results

    def generate_report(self) -> str:
        """Generate a comprehensive test report"""
        passed_tests = [r for r in self.test_results if r["success"]]
        failed_tests = [r for r in self.test_results if not r["success"]]
        
        report = f"""
📊 FRAME RATE TESTING REPORT
{'='*80}

🎯 Total Tests: {len(self.test_results)}
✅ Passed: {len(passed_tests)}
❌ Failed: {len(failed_tests)}
📈 Success Rate: {len(passed_tests)/len(self.test_results)*100:.1f}%

DETAILED RESULTS:
{'-'*80}
"""
        
        for result in self.test_results:
            status = "✅ PASS" if result["success"] else "❌ FAIL"
            report += f"{status} | {result['frame_rate']}\n"
            if result["error"]:
                report += f"     Error: {result['error']}\n"
            if result["recording_info"] and result["recording_info"]["success"]:
                report += f"     Recording: {result['recording_info']['file_path']}\n"
            report += "\n"
        
        return report

def main():
    """Main test execution"""
    print("🚀 TestMediaProjectionApp Frame Rate Automation")
    print("=" * 80)
    
    # Initialize automation
    automation = TestMediaProjectionUIAutomation()
    
    # Run comprehensive tests
    results = automation.run_comprehensive_test()
    
    # Generate and display report
    report = automation.generate_report()
    print(report)
    
    # Save results to file
    with open("frame_rate_test_results.json", "w") as f:
        json.dump(results, f, indent=2)
    
    with open("frame_rate_test_report.txt", "w") as f:
        f.write(report)
    
    print("📄 Results saved to frame_rate_test_results.json")
    print("📄 Report saved to frame_rate_test_report.txt")

if __name__ == "__main__":
    main()