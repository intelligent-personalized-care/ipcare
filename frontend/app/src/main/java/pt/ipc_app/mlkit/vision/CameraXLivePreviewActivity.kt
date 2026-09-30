/*

 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pt.ipc_app.mlkit.vision

import pt.ipc_app.R

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.video.VideoCapture
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.VideoRecordEvent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.util.Size
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import pt.ipc_app.domain.exercise.Exercise
import pt.ipc_app.feedback.ExerciseVoiceFeedback
import pt.ipc_app.mlkit.GraphicOverlay
import pt.ipc_app.mlkit.posedetector.CameraPhase
import pt.ipc_app.mlkit.posedetector.PoseDetectorProcessor
import pt.ipc_app.ui.theme.AppTheme
import pt.ipc_app.utils.viewModelInit
import java.io.File
import java.util.UUID

/** Camera preview, inference and recording share the same lifecycle and lens. */
@SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError")
class CameraXLivePreviewActivity : ComponentActivity() {
    private val viewModel by viewModels<CameraXLiveViewModel> { viewModelInit { CameraXLiveViewModel(applicationContext, exercise) } }
    private lateinit var voice: ExerciseVoiceFeedback
    private var provider: ProcessCameraProvider? = null
    private var preview: PreviewView? = null
    private var overlay: GraphicOverlay? = null
    private var processor: PoseDetectorProcessor? = null
    private var video: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var permissionRequested = false
    private var showPermissionHelp by mutableStateOf(false)
    private var lens = CameraSelector.LENS_FACING_BACK
    private var captureActive = false
    private var stopping = false
    private var keepRecording = false
    private var cameraReady by mutableStateOf(false)
    private var cameraError by mutableStateOf<String?>(null)
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasPermissions()) { cameraError = null; bindCamera() }
        else { cameraReady = false; cameraError = getString(R.string.feedback_camera_permission_is_required_to_monitor_your_exercise_tap_try_ag) }
    }

    private val galleryPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.exportGallery()
    }
    private fun saveToGallery() {
        if (CameraGallery.permitted(this)) viewModel.exportGallery()
        else galleryPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionRequested = savedInstanceState?.getBoolean("cameraPermissionRequested") ?: false
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        voice = ExerciseVoiceFeedback(this)
        lifecycle.addObserver(voice)
        setContent {
            AppTheme {
                val state by viewModel.state.collectAsState()
                val voiceEnabled by voice.enabled.collectAsState()
                var confirmExit by remember { mutableStateOf(false) }
                fun exit() { if (state.recording) confirmExit = true else finish() }
                BackHandler(enabled = true) { if (!state.saving && !state.finalizing) exit() }
                CameraExerciseScreen(state, exercise.exeTitle, cameraReady, cameraError, voiceEnabled,
                    onVoice = voice::setEnabled, onExit = { exit() }, onFlip = {
                        lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                        viewModel.invalidateCalibration(); bindCamera()
                    }, onSide = viewModel::chooseSide, onCalibrate = viewModel::calibrate,
                    onRecord = { if (captureActive) stopCapture(false) else if (viewModel.state.value.autoStartPending) viewModel.cancelAutoStart() else viewModel.prepareStart() }, onSave = viewModel::save, onWithLoad = viewModel::setWithLoad, onLoadText = viewModel::setLoadText,
                    onRetry = { viewModel.load(); requestCamera() },
                    onSaveLocally = viewModel::saveLocally, onRetryUploads = viewModel::retryPendingUploads, onGallery = ::saveToGallery) {
                    AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
                        FrameLayout(context).apply {
                            preview = PreviewView(context).also { it.scaleType = PreviewView.ScaleType.FILL_CENTER; addView(it, FrameLayout.LayoutParams(-1, -1)) }
                            this@CameraXLivePreviewActivity.overlay = GraphicOverlay(context, null).also { addView(it, FrameLayout.LayoutParams(-1, -1)) }
                            bindCamera()
                        }
                    })
                }
                if (showPermissionHelp) AlertDialog(
                    onDismissRequest = { showPermissionHelp = false },
                    title = { Text(getString(R.string.feedback_allow_camera_access)) },
                    text = { Text(getString(R.string.feedback_the_camera_detects_movement_and_records_your_set_microphone_acces)) },
                    confirmButton = { TextButton(onClick = {
                        showPermissionHelp = false
                        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) permissions.launch(REQUIRED)
                        else startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                    }) { Text(if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) getString(R.string.feedback_allow) else getString(R.string.feedback_open_settings)) } },
                    dismissButton = { TextButton(onClick = { showPermissionHelp = false }) { Text(getString(R.string.feedback_not_now)) } })
                if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false }, title = { Text(getString(R.string.feedback_stop_this_set)) },
                    text = { Text(getString(R.string.feedback_this_set_is_not_complete_and_will_need_to_be_repeated)) },
                    confirmButton = { TextButton(onClick = { confirmExit = false; stopCapture(false); finish() }) { Text(getString(R.string.feedback_exit)) } },
                    dismissButton = { TextButton(onClick = { confirmExit = false }) { Text(getString(R.string.feedback_continue)) } })
            }
        }
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try { provider = future.get(); requestCamera() }
            catch (e: Exception) { cameraError = getString(R.string.feedback_unable_to_start_the_camera_try_again) }
        }, ContextCompat.getMainExecutor(this))
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                var previousReps = 0
                var previousPhase: CameraPhase? = null
                var previousCountdown: Int? = null
                launch {
                    voice.unavailable.collect { if (it) android.widget.Toast.makeText(this@CameraXLivePreviewActivity,
                        getString(R.string.feedback_voice_unavailable_enable_a_voice_for_the_app_language_in_text_to), android.widget.Toast.LENGTH_LONG).show() }
                }
                viewModel.state.collect { state ->
                    val seconds = state.startCountdown
                    if (seconds != previousCountdown && seconds != null && seconds > 0) voice.speak("$seconds", true)
                    previousCountdown = seconds
                    if (seconds == 0 && cameraReady && !captureActive && !stopping) {
                        startCapture()
                        if (captureActive) voice.speak(getString(R.string.feedback_begin), true)
                    }
                    val phase = state.measurement.phase
                    if (captureActive && state.measurement.repetitions > previousReps) voice.speak(getString(R.string.voice_repetition, state.measurement.repetitions), true)
                    previousReps = state.measurement.repetitions
                    if (seconds == null && phase != previousPhase && (state.recording || phase == CameraPhase.CALIBRATING || phase == CameraPhase.READY)) {
                        voice.speak(getString(cameraInstruction(state)), phase == CameraPhase.LIMIT)
                    }
                    previousPhase = phase
                    if (captureActive && phase == CameraPhase.COMPLETE) stopCapture(true)
                }
            }
        }
    }

    private fun hasPermissions() = REQUIRED.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    private fun requestCamera() {
        when {
            hasPermissions() -> bindCamera()
            permissionRequested || shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> showPermissionHelp = true
            else -> { permissionRequested = true; permissions.launch(REQUIRED) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun bindCamera() {
        val provider = provider ?: return
        val preview = preview ?: return
        val overlay = overlay ?: return
        if (!hasPermissions()) { cameraReady = false; cameraError = getString(R.string.feedback_allow_camera_access_to_continue); return }
        if (captureActive || stopping) return
        cameraReady = false; cameraError = null
        try {
            provider.unbindAll(); processor?.stop(); processor = null; video = null
            val selector = CameraSelector.Builder().requireLensFacing(lens).build()
            if (!provider.hasCamera(selector)) { cameraError = getString(R.string.feedback_this_camera_is_unavailable_switch_cameras); return }
            val rotation = preview.display?.rotation ?: android.view.Surface.ROTATION_0
            val previewUseCase = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).setTargetRotation(rotation).build()
            previewUseCase.setSurfaceProvider(preview.surfaceProvider)
            val detector = PoseDetectorProcessor(this, PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build(), viewModel)
            processor = detector
            val analysis = ImageAnalysis.Builder().setTargetResolution(Size(640, 480)).setTargetRotation(rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { image ->
                val rotated = image.imageInfo.rotationDegrees % 180 != 0
                overlay.setImageSourceInfo(if (rotated) image.height else image.width, if (rotated) image.width else image.height,
                    lens == CameraSelector.LENS_FACING_FRONT)
                try { detector.processImageProxy(image, overlay) }
                catch (e: Exception) { image.close(); viewModel.trackingLost() }
            }
            val recorder = Recorder.Builder()
                .setTargetVideoEncodingBitRate(1_000_000)
                .setQualitySelector(QualitySelector.fromOrderedList(listOf(Quality.SD, Quality.HD), FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
                .build()
            val capture = VideoCapture.withOutput(recorder)
            capture.targetRotation = rotation
            provider.bindToLifecycle(this, selector, previewUseCase, analysis, capture)
            video = capture
            cameraReady = true
        } catch (e: Exception) {
            Log.e("ExerciseCamera", "Unable to bind camera use cases (lens=$lens)", e)
            provider.unbindAll(); processor?.stop(); processor = null; video = null
            cameraError = if (e is SecurityException) getString(R.string.feedback_camera_permission_was_removed_allow_access_and_try_again)
                else getString(R.string.feedback_unable_to_start_this_camera_close_other_apps_using_it_and_try_aga)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startCapture() {
        val capture = video ?: return
        if (!cameraReady || stopping || captureActive || !viewModel.start()) return
        val directory = File(filesDir, "camera_sessions").apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.mp4")
        captureActive = true; keepRecording = false
        try {
            // Exercise recordings need images only; microphone denial never blocks execution.
            recording = capture.output.prepareRecording(this, FileOutputOptions.Builder(file).build())
                .start(ContextCompat.getMainExecutor(this)) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        recording = null; captureActive = false; stopping = false
                        if (!event.hasError()) {
                            viewModel.videoSaved(file, keepRecording)
                            if (keepRecording && !CameraGallery.permitted(this)) saveToGallery()
                            voice.speak(if (keepRecording) getString(R.string.feedback_set_complete_save_the_video_to_continue) else getString(R.string.feedback_set_stopped), true)
                        } else {
                            Log.e("ExerciseCamera", "Recording failed: ${event.error}", event.cause)
                            file.delete()
                            viewModel.captureFailed(getString(R.string.feedback_unable_to_save_the_recording_check_free_storage_and_try_again))
                        }
                    }
                }
        } catch (e: Exception) {
            captureActive = false; file.delete(); viewModel.captureFailed(getString(R.string.feedback_unable_to_start_recording))
        }
    }
    private fun stopCapture(complete: Boolean) {
        if (!captureActive || stopping) return
        keepRecording = complete; stopping = true
        viewModel.stopCapture()
        recording?.stop()
    }
    override fun onPause() {
        if (captureActive) stopCapture(viewModel.state.value.measurement.phase == CameraPhase.COMPLETE)
        viewModel.invalidateCalibration()
        super.onPause()
    }
    override fun onResume() {
        super.onResume()
        if (!captureActive && !stopping) bindCamera()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("cameraPermissionRequested", permissionRequested)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        recording?.close()
        provider?.unbindAll()
        processor?.stop()
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private val exercise: Exercise by lazy {
        checkNotNull(if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(EXERCISE, Exercise::class.java) else intent.getParcelableExtra(EXERCISE))
    }
    companion object {
        const val EXERCISE = "EXERCISE_TO_DO"
        private val REQUIRED = arrayOf(Manifest.permission.CAMERA)
        fun navigate(context: Context, exercise: Exercise) = context.startActivity(Intent(context, CameraXLivePreviewActivity::class.java).putExtra(EXERCISE, exercise))
    }
}
