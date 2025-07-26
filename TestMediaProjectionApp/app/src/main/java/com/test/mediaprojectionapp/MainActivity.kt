package com.test.mediaprojectionapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.test.mediaprojectionapp.databinding.ActivityMainBinding
import com.t34400.mediaprojectionlib.recording.VideoRecordingManager
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var videoRecordingManager: VideoRecordingManager? = null
    private var isRecording = false

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        binding.buttonStartRecording.setOnClickListener {
            if (!isRecording) {
                startRecording()
            } else {
                stopRecording()
            }
        }

        binding.buttonTestCapture.setOnClickListener {
            testScreenCapture()
        }

        updateUI()
    }

    private fun checkPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            initializeVideoRecordingManager()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            
            if (allPermissionsGranted) {
                initializeVideoRecordingManager()
                showToast("Permissions granted")
            } else {
                showToast("Permissions required for screen recording")
                Log.w(TAG, "Some permissions were denied")
            }
        }
    }

    private fun initializeVideoRecordingManager() {
        // Using service-based approach, so just mark as ready
        binding.textStatus.text = "Ready to test MediaProjection"
        Log.d(TAG, "Test app initialized - using VideoRecordingService")
    }

    private fun startRecording() {
        try {
            binding.textStatus.text = "Starting recording..."
            showToast("Starting screen recording...")
            
            // Use the VideoRecordingService to start recording
            com.t34400.mediaprojectionlib.recording.VideoRecordingService.startRecording(
                context = this,
                videoBitrate = 5_000_000,  // 5 Mbps
                videoFrameRate = 30,       // 30 fps
                outputDirectory = "",      // Use default
                maxDurationMs = -1L        // Unlimited
            )
            
            isRecording = true
            updateUI()
            Log.d(TAG, "Recording start initiated via service")

        } catch (e: Exception) {
            Log.e(TAG, "Error starting recording", e)
            showToast("Error starting recording: ${e.message}")
        }
    }

    private fun stopRecording() {
        try {
            binding.textStatus.text = "Stopping recording..."
            showToast("Stopping screen recording...")
            
            // Use the VideoRecordingService to stop recording
            com.t34400.mediaprojectionlib.recording.VideoRecordingService.stopRecording(this)
            
            isRecording = false
            updateUI()
            Log.d(TAG, "Recording stop initiated via service")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            showToast("Error stopping recording: ${e.message}")
        }
    }

    private fun testScreenCapture() {
        // Test MediaProjection permission by starting the service briefly
        try {
            binding.textStatus.text = "Testing MediaProjection permission..."
            showToast("Starting MediaProjection test...")
            
            // Start the video recording service to trigger permission flow
            com.t34400.mediaprojectionlib.recording.VideoRecordingService.startService(this)
            
            // Give some time for the service to initialize, then show success
            binding.root.postDelayed({
                binding.textStatus.text = "Screen capture test: Service started successfully"
                showToast("MediaProjection permission test completed!")
                Log.d(TAG, "Screen capture test: Service started")
            }, 2000)
            
        } catch (e: Exception) {
            binding.textStatus.text = "Screen capture test: ERROR - ${e.message}"
            showToast("Screen capture test failed: ${e.message}")
            Log.e(TAG, "Screen capture test failed", e)
        }
    }

    private fun updateUI() {
        binding.buttonStartRecording.text = if (isRecording) "Stop Recording" else "Start Recording"
        binding.buttonStartRecording.isEnabled = true
        binding.buttonTestCapture.isEnabled = true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the service when app is destroyed
        com.t34400.mediaprojectionlib.recording.VideoRecordingService.stopService(this)
        Log.d(TAG, "MainActivity destroyed, VideoRecordingService stopped")
    }
}