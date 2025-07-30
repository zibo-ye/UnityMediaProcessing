package com.test.mediaprojectionapp

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.test.mediaprojectionapp.databinding.ActivityMainBinding
import com.t34400.mediaprojectionlib.recording.VideoRecordingManager
import com.t34400.mediaprojectionlib.recording.VideoRecordingService
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var videoRecordingManager: VideoRecordingManager? = null
    private var isRecording = false
    private var recordingStartTime: Long = 0
    private var outputFilePath: String? = null
    
    // UI update handler
    private val uiHandler = Handler(Looper.getMainLooper())
    private val updateStatusRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                updateRecordingDuration()
                uiHandler.postDelayed(this, 1000) // Update every second
            }
        }
    }
    
    // MediaProjection permission launcher
    private lateinit var mediaProjectionLauncher: ActivityResultLauncher<Intent>
    
    // Store permission data for reuse
    private var mediaProjectionResultCode: Int = -1
    private var mediaProjectionResultData: Intent? = null
    
    // Configuration data classes
    data class CodecOption(val codec: VideoRecordingManager.SupportedCodec, val displayName: String)
    data class ResolutionOption(val width: Int, val height: Int, val displayName: String)
    data class BitrateOption(val bitrate: Int, val displayName: String)
    data class FrameRateOption(val frameRate: Int, val displayName: String)
    
    // Configuration options
    private var availableCodecs = listOf<CodecOption>()
    private var availableResolutions = listOf<ResolutionOption>()
    private var availableBitrates = listOf<BitrateOption>()
    private var availableFrameRates = listOf<FrameRateOption>()
    
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.POST_NOTIFICATIONS
        )
        
        // Broadcasting actions for service communication
        const val ACTION_RECORDING_STATE_CHANGED = "com.test.mediaprojectionapp.RECORDING_STATE_CHANGED"
        const val ACTION_RECORDING_COMPLETE = "com.test.mediaprojectionapp.RECORDING_COMPLETE"
        const val ACTION_RECORDING_ERROR = "com.test.mediaprojectionapp.RECORDING_ERROR"
        
        const val EXTRA_RECORDING_STATE = "extra_recording_state"
        const val EXTRA_OUTPUT_PATH = "extra_output_path"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"
    }
    
    /**
     * Test app specific resolutions for comprehensive testing
     */
    private fun getTestResolutions(): List<Pair<Int, Int>> {
        return listOf(
            // 4K and high resolutions
            Pair(3840, 2160), // 4K UHD
            Pair(2560, 1440), // QHD
            
            // Standard resolutions
            Pair(1920, 1080), // FHD
            Pair(1280, 720),  // HD
            Pair(854, 480),   // 480p
            
            // VR-specific resolutions for testing
            Pair(3840, 2160), // 4K UHD - Premium VR quality
            Pair(2048, 1024), // High-res ultra-wide VR (2:1)
            Pair(1024, 512),  // Ultra-wide VR (2:1 aspect ratio)
            
            // Ultra-wide resolutions
            Pair(3440, 1440), // Ultra-wide QHD
            Pair(2560, 1080), // Ultra-wide FHD
            
            // Square resolutions for testing
            Pair(1440, 1440), // 1:1 aspect ratio
            Pair(1080, 1080)  // 1:1 aspect ratio (lower res)
        ).distinct()
    }
    
    /**
     * Test app specific frame rates including VR and high refresh rates
     */
    private fun getTestFrameRates(deviceFrameRates: List<Int>): List<FrameRateOption> {
        // Test app specific frame rates for comprehensive testing
        val testFrameRates = listOf(
            FrameRateOption(24, "24 FPS (Cinema)"),
            FrameRateOption(25, "25 FPS (PAL)"),
            FrameRateOption(30, "30 FPS (Standard)"),
            FrameRateOption(36, "36 FPS (Cinema+)"),
            FrameRateOption(48, "48 FPS (High Cinema)"),
            FrameRateOption(50, "50 FPS (PAL High)"),
            FrameRateOption(60, "60 FPS (Smooth)"),
            FrameRateOption(72, "72 FPS (VR Standard)"),
            FrameRateOption(80, "80 FPS (High Performance)"),
            FrameRateOption(90, "90 FPS (VR Premium)"),
            FrameRateOption(120, "120 FPS (Ultra High)")
        )
        
        // Filter to only include frame rates supported by the device
        return testFrameRates.filter { testRate ->
            deviceFrameRates.contains(testRate.frameRate) || 
            testRate.frameRate <= 60 // Always include basic rates
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMediaProjectionLauncher()
        setupBroadcastReceiver()
        setupSpinners()
        setupUI()
        checkPermissions()
        
        Log.d(TAG, "MainActivity created")
    }

    /**
     * Setup MediaProjection permission launcher
     */
    private fun setupMediaProjectionLauncher() {
        mediaProjectionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null) {
                    Log.d(TAG, "MediaProjection permission granted")
                    
                    // Store permission data for reuse
                    mediaProjectionResultCode = result.resultCode
                    mediaProjectionResultData = data
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android 14+ approach: Start service with permission data
                        startVideoRecordingService(result.resultCode, data)
                    } else {
                        // Older Android versions: Direct VideoRecordingManager usage
                        startDirectRecording(result.resultCode, data)
                    }
                } else {
                    showToast("MediaProjection permission data is null")
                }
            } else {
                showToast("MediaProjection permission denied")
                binding.textStatus.text = "Permission denied - cannot record"
            }
        }
    }
    
    /**
     * Setup broadcast receiver for service communication
     */
    private fun setupBroadcastReceiver() {
        val filter = IntentFilter().apply {
            addAction(ACTION_RECORDING_STATE_CHANGED)
            addAction(ACTION_RECORDING_COMPLETE)
            addAction(ACTION_RECORDING_ERROR)
        }
        
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceReceiver, filter)
        Log.d(TAG, "Broadcast receiver registered")
    }
    
    /**
     * Broadcast receiver for service updates
     */
    private val serviceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_RECORDING_STATE_CHANGED -> {
                    val state = intent.getStringExtra(EXTRA_RECORDING_STATE)
                    handleRecordingStateChanged(state)
                }
                ACTION_RECORDING_COMPLETE -> {
                    val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH)
                    handleRecordingComplete(outputPath)
                }
                ACTION_RECORDING_ERROR -> {
                    val errorMessage = intent.getStringExtra(EXTRA_ERROR_MESSAGE)
                    handleRecordingError(errorMessage)
                }
            }
        }
    }
    
    /**
     * Setup configuration spinners
     */
    private fun setupSpinners() {
        try {
            // Create VideoRecordingManager to get configuration options
            val recordingManager = VideoRecordingManager(this)
            
            // Setup codec spinner
            val codecList = recordingManager.getAvailableCodecs()
            availableCodecs = codecList.map { CodecOption(it, it.displayName) }
            val codecAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, 
                availableCodecs.map { it.displayName })
            codecAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCodec.adapter = codecAdapter
            
            // Setup resolution spinner with test app specific resolutions
            val resolutionList = getTestResolutions()
            availableResolutions = resolutionList.map { 
                ResolutionOption(it.first, it.second, "${it.first}x${it.second}") 
            }
            val resolutionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
                availableResolutions.map { it.displayName })
            resolutionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerResolution.adapter = resolutionAdapter
            
            // Setup bitrate spinner
            availableBitrates = listOf(
                BitrateOption(1_000_000, "1 Mbps (Low)"),
                BitrateOption(2_500_000, "2.5 Mbps (Medium)"),
                BitrateOption(5_000_000, "5 Mbps (High)"),
                BitrateOption(10_000_000, "10 Mbps (Very High)"),
                BitrateOption(15_000_000, "15 Mbps (Ultra)"),
                BitrateOption(25_000_000, "25 Mbps (Premium)"),
                BitrateOption(50_000_000, "50 Mbps (Maximum)")
            )
            val bitrateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
                availableBitrates.map { it.displayName })
            bitrateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerBitrate.adapter = bitrateAdapter
            
            // Setup frame rate spinner with test app specific rates
            val availableFrameRatesFromDevice = recordingManager.getAvailableFrameRates()
            availableFrameRates = getTestFrameRates(availableFrameRatesFromDevice)
            val frameRateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item,
                availableFrameRates.map { it.displayName })
            frameRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerFrameRate.adapter = frameRateAdapter
            
            // Set default selections
            binding.spinnerCodec.setSelection(0) // First available codec
            binding.spinnerResolution.setSelection(2) // Usually 1920x1080
            binding.spinnerBitrate.setSelection(2) // 5 Mbps
            binding.spinnerFrameRate.setSelection(0) // 30 FPS
            
            Log.d(TAG, "Spinners configured with ${availableCodecs.size} codecs, ${availableResolutions.size} resolutions, ${availableBitrates.size} bitrates, ${availableFrameRates.size} frame rates")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up spinners", e)
            showToast("Error setting up configuration: ${e.message}")
        }
    }
    
    private fun setupUI() {
        binding.buttonStartRecording.setOnClickListener {
            if (!isRecording) {
                // Always request fresh MediaProjection permission for each recording
                // MediaProjection tokens are often single-use and become invalid after stopping
                Log.d(TAG, "Requesting fresh MediaProjection permission")
                requestMediaProjectionPermission()
            } else {
                stopRecording()
            }
        }

        binding.buttonTestCapture.setOnClickListener {
            testScreenCapture()
        }

        binding.buttonQueryCodecs.setOnClickListener {
            queryAvailableCodecs()
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

    /**
     * Request MediaProjection permission
     */
    private fun requestMediaProjectionPermission() {
        try {
            binding.textStatus.text = "Requesting screen capture permission..."
            
            val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
                as android.media.projection.MediaProjectionManager
            val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
            
            Log.d(TAG, "Launching MediaProjection permission request")
            mediaProjectionLauncher.launch(captureIntent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting MediaProjection permission", e)
            showToast("Error requesting permission: ${e.message}")
            binding.textStatus.text = "Error requesting permission"
        }
    }
    
    /**
     * Start video recording service (Android 14+ approach)
     */
    private fun startVideoRecordingService(resultCode: Int, data: Intent) {
        try {
            // Get selected configuration values
            val selectedCodec = availableCodecs.getOrNull(binding.spinnerCodec.selectedItemPosition)
            val selectedResolution = availableResolutions.getOrNull(binding.spinnerResolution.selectedItemPosition)
            val selectedBitrate = availableBitrates.getOrNull(binding.spinnerBitrate.selectedItemPosition)
            val selectedFrameRate = availableFrameRates.getOrNull(binding.spinnerFrameRate.selectedItemPosition)
            
            // Use defaults if nothing selected
            val codec = selectedCodec?.codec?.mimeType ?: VideoRecordingManager.SupportedCodec.H264.mimeType
            val width = selectedResolution?.width ?: 1920
            val height = selectedResolution?.height ?: 1080
            val bitrate = selectedBitrate?.bitrate ?: 5_000_000
            val frameRate = selectedFrameRate?.frameRate ?: 30
            
            binding.textStatus.text = "Starting recording: ${width}x${height}, ${selectedBitrate?.displayName ?: "5 Mbps"}, ${selectedFrameRate?.displayName ?: "30 FPS"}, ${selectedCodec?.displayName ?: "H.264"}"
            
            val serviceIntent = Intent(this, VideoRecordingService::class.java).apply {
                action = VideoRecordingService.ACTION_START_RECORDING
                putExtra(VideoRecordingService.EXTRA_VIDEO_BITRATE, bitrate)
                putExtra(VideoRecordingService.EXTRA_VIDEO_FRAMERATE, frameRate) // Selected frame rate
                putExtra(VideoRecordingService.EXTRA_OUTPUT_DIRECTORY, "")     // Default
                putExtra(VideoRecordingService.EXTRA_MAX_DURATION_MS, -1L)     // Unlimited
                putExtra("resultCode", resultCode)
                putExtra("data", data)
                // Add custom resolution and codec
                putExtra("videoWidth", width)
                putExtra("videoHeight", height)
                putExtra("videoCodec", codec)
            }
            
            Log.d(TAG, "Sending permission data to service: resultCode=$resultCode, data=$data")
            
            ContextCompat.startForegroundService(this, serviceIntent)
            
            // Update UI immediately
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            updateUI()
            startStatusUpdates()
            
            Log.d(TAG, "VideoRecordingService started with MediaProjection permission")
            showToast("Screen recording started!")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting VideoRecordingService", e)
            showToast("Error starting recording: ${e.message}")
            binding.textStatus.text = "Failed to start recording"
        }
    }
    
    /**
     * Start direct recording (for older Android versions)
     */
    private fun startDirectRecording(resultCode: Int, data: Intent) {
        try {
            // For older versions, could implement direct VideoRecordingManager usage
            // For now, fallback to service approach
            startVideoRecordingService(resultCode, data)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error starting direct recording", e)
            showToast("Error starting recording: ${e.message}")
        }
    }

    private fun stopRecording() {
        try {
            binding.textStatus.text = "Stopping recording..."
            showToast("Stopping screen recording...")
            
            // Stop the VideoRecordingService
            VideoRecordingService.stopRecording(this)
            
            // Update UI
            isRecording = false
            uiHandler.removeCallbacks(updateStatusRunnable)
            updateUI()
            
            Log.d(TAG, "Recording stop initiated via service")

        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            showToast("Error stopping recording: ${e.message}")
        }
    }

    private fun testScreenCapture() {
        try {
            binding.textStatus.text = "Testing MediaProjection permission..."
            showToast("Testing MediaProjection permission...")
            
            // Simply request MediaProjection permission to test the flow
            requestMediaProjectionPermission()
            
        } catch (e: Exception) {
            binding.textStatus.text = "Screen capture test: ERROR - ${e.message}"
            showToast("Screen capture test failed: ${e.message}")
            Log.e(TAG, "Screen capture test failed", e)
        }
    }

    /**
     * Query and display available hardware codecs
     */
    private fun queryAvailableCodecs() {
        try {
            binding.textStatus.text = "Querying available hardware codecs..."
            
            // Create a VideoRecordingManager to query codecs
            val recordingManager = VideoRecordingManager(this)
            
            // Get available codecs
            val availableCodecs = recordingManager.getAvailableCodecs()
            val commonResolutions = recordingManager.getCommonResolutions()
            
            // Build result string
            val codecsText = availableCodecs.joinToString(", ") { it.displayName }
            val resolutionsText = commonResolutions.take(5).joinToString(", ") { "${it.first}x${it.second}" }
            
            // Get recommended bitrates for common resolutions and frame rates
            val bitrateInfo = StringBuilder()
            for (res in commonResolutions.take(3)) {
                val bitrate30fps = recordingManager.getRecommendedBitrate(res.first, res.second, 30)
                val bitrate60fps = recordingManager.getRecommendedBitrate(res.first, res.second, 60)
                val bitrate90fps = recordingManager.getRecommendedBitrate(res.first, res.second, 90)
                bitrateInfo.append("${res.first}x${res.second}: ${bitrate30fps/1_000_000}Mbps@30fps, ${bitrate60fps/1_000_000}Mbps@60fps, ${bitrate90fps/1_000_000}Mbps@90fps; ")
            }
            
            // Test frame rate presets
            val frameRateList = availableFrameRates.joinToString(", ") { "${it.frameRate}fps" }
            
            val resultMessage = """
                Hardware Codecs: $codecsText
                
                Common Resolutions: $resolutionsText
                
                Frame Rate Presets: $frameRateList
                
                Recommended Bitrates: ${bitrateInfo.toString().trimEnd(',', ' ', ';')}
            """.trimIndent()
            
            binding.textStatus.text = "Codec query complete!"
            binding.textOutputPath.text = "Available Codecs: $codecsText"
            binding.textFileInfo.text = "Common Resolutions: $resolutionsText"
            
            showToast("Found ${availableCodecs.size} hardware codecs")
            Log.i(TAG, "Codec query results:\n$resultMessage")
            
        } catch (e: Exception) {
            binding.textStatus.text = "Codec query failed: ${e.message}"
            showToast("Codec query failed: ${e.message}")
            Log.e(TAG, "Codec query failed", e)
        }
    }

    /**
     * Update recording duration display
     */
    private fun updateRecordingDuration() {
        if (isRecording && recordingStartTime > 0) {
            val duration = (System.currentTimeMillis() - recordingStartTime) / 1000
            val minutes = duration / 60
            val seconds = duration % 60
            binding.textStatus.text = String.format("Recording... %02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Start periodic status updates
     */
    private fun startStatusUpdates() {
        uiHandler.removeCallbacks(updateStatusRunnable)
        uiHandler.post(updateStatusRunnable)
    }
    
    /**
     * Handle recording state changes from service
     */
    private fun handleRecordingStateChanged(state: String?) {
        Log.d(TAG, "Recording state changed: $state")
        
        when (state) {
            "PREPARING" -> {
                binding.textStatus.text = "Preparing to record..."
            }
            "RECORDING" -> {
                isRecording = true
                recordingStartTime = System.currentTimeMillis()
                binding.textStatus.text = "Recording started!"
                startStatusUpdates()
                updateUI()
            }
            "STOPPING" -> {
                binding.textStatus.text = "Stopping recording..."
            }
            "IDLE" -> {
                isRecording = false
                uiHandler.removeCallbacks(updateStatusRunnable)
                binding.textStatus.text = "Recording stopped"
                updateUI()
            }
            "ERROR" -> {
                isRecording = false
                uiHandler.removeCallbacks(updateStatusRunnable)
                binding.textStatus.text = "Recording error occurred"
                updateUI()
            }
        }
    }
    
    /**
     * Handle recording completion
     */
    private fun handleRecordingComplete(outputPath: String?) {
        Log.i(TAG, "Recording completed: $outputPath")
        
        outputFilePath = outputPath
        isRecording = false
        uiHandler.removeCallbacks(updateStatusRunnable)
        
        if (outputPath != null) {
            val file = File(outputPath)
            val fileName = file.name
            val fileSize = if (file.exists()) "${file.length() / 1024 / 1024} MB" else "Unknown size"
            
            binding.textStatus.text = "Recording completed!"
            binding.textOutputPath.text = "Output: $fileName"
            binding.textFileInfo.text = "File size: $fileSize"
            
            showToast("Recording saved: $fileName")
            Log.i(TAG, "Recording saved to: $outputPath (${fileSize})")
        } else {
            binding.textStatus.text = "Recording completed (no output path)"
            binding.textOutputPath.text = "Output: Unknown"
        }
        
        updateUI()
    }
    
    /**
     * Handle recording errors
     */
    private fun handleRecordingError(errorMessage: String?) {
        Log.e(TAG, "Recording error: $errorMessage")
        
        isRecording = false
        uiHandler.removeCallbacks(updateStatusRunnable)
        
        binding.textStatus.text = "Error: ${errorMessage ?: "Unknown error"}"
        showToast("Recording failed: ${errorMessage ?: "Unknown error"}")
        
        updateUI()
    }
    
    private fun updateUI() {
        binding.buttonStartRecording.text = if (isRecording) "Stop Recording" else "Start Recording"
        binding.buttonStartRecording.isEnabled = true
        binding.buttonTestCapture.isEnabled = !isRecording
        
        // Update output info if available
        if (outputFilePath != null && !isRecording) {
            val file = File(outputFilePath!!)
            binding.textOutputPath.text = "Output: ${file.name}"
            if (file.exists()) {
                val fileSize = file.length() / 1024 / 1024
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(file.lastModified()))
                binding.textFileInfo.text = "File: ${fileSize} MB, Created: $timestamp"
            }
        } else if (!isRecording) {
            binding.textOutputPath.text = "Output: (no recording yet)"
            binding.textFileInfo.text = "File info: (no file yet)"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Stop status updates
        uiHandler.removeCallbacks(updateStatusRunnable)
        
        // Unregister broadcast receiver
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering receiver", e)
        }
        
        // Stop the service when app is destroyed
        VideoRecordingService.stopService(this)
        Log.d(TAG, "MainActivity destroyed, VideoRecordingService stopped")
    }
}