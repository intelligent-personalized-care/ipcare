package pt.ipc_app.mlkit.vision

import pt.ipc_app.R

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.pose.Pose
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.domain.exercise.Exercise
import pt.ipc_app.domain.exercise.ExerciseInfo
import pt.ipc_app.domain.exercise.ExerciseTotalInfo
import pt.ipc_app.mlkit.posedetector.*
import pt.ipc_app.service.models.exercises.SensorProfile
import pt.ipc_app.ui.screens.AppViewModel
import pt.ipc_app.utils.executeRequest
import java.io.File


data class CameraExerciseState(
    val loading: Boolean = true,
    val freePractice: Boolean = false,
    val error: String? = null,
    val info: ExerciseInfo? = null,
    val profile: SensorProfile? = null,
    val joint: CameraJoint? = null,
    val movement: CameraMovement? = null,
    val side: BodySide = BodySide.LEFT,
    val measurement: CameraMeasurement = CameraMeasurement(),
    val autoStartPending: Boolean = false,
    val startCountdown: Int? = null,
    val recording: Boolean = false,
    val finalizing: Boolean = false,
    val elapsedSeconds: Int = 0,
    val currentSet: Int = 1,
    val targetSets: Int = 1,
    val targetReps: Int = 10,
    val pendingVideo: String? = null,
    val saving: Boolean = false,
    val pendingUploads: Int = 0,
    val galleryMessage: String? = null,
    val withLoad: Boolean? = null,
    val loadText: String = "",
    val loadSelectionLocked: Boolean = false,
    val complete: Boolean = false,
    val message: String? = null
)

class CameraXLiveViewModel(private val context: Context, private val exercise: Exercise) : AppViewModel() {
    private val dependencies = context.applicationContext as DependenciesContainer
    private val preferences = context.getSharedPreferences("pending_camera_sessions", Context.MODE_PRIVATE)
    private val planned = exercise as? ExerciseTotalInfo
    private val owner = dependencies.sessionManager.userLoggedIn.id
    private val baseKey = "${owner}_${planned?.planId}_${planned?.dailyListId}_${planned?.exercise?.id}"
    private val key get() = setKey(state.value.currentSet)
    private fun setKey(set: Int) = "${baseKey}_video_$set"
    private fun deferredSets(): Set<Int> = preferences.getStringSet("${baseKey}_deferred", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    private fun defer(set: Int) = preferences.edit().putStringSet("${baseKey}_deferred", (deferredSets() + set).map { it.toString() }.toSet()).commit()
    private fun forget(set: Int) {
        val entry = setKey(set)
        check(preferences.edit().remove(entry).remove("${entry}_load").remove("${entry}_weight").remove("${entry}_submitted")
            .putStringSet("${baseKey}_deferred", (deferredSets() - set).map { it.toString() }.toSet()).commit())
    }
    private fun migratePendingVideo() {
        val old = preferences.getString(baseKey, null) ?: return
        val set = preferences.getInt("${baseKey}_set", 0)
        if (set < 1) return
        val entry = setKey(set)
        val edit = preferences.edit()
        if (!preferences.contains(entry)) {
            edit.putString(entry, old).putString("${entry}_weight", preferences.getString("${baseKey}_weight", ""))
                .putBoolean("${entry}_submitted", preferences.getBoolean("${baseKey}_submitted", false))
            if (preferences.contains("${baseKey}_load")) edit.putBoolean("${entry}_load", preferences.getBoolean("${baseKey}_load", false))
        }
        check(edit.remove(baseKey).remove("${baseKey}_set").remove("${baseKey}_load").remove("${baseKey}_weight").remove("${baseKey}_submitted").commit())
    }
    private val _state = MutableStateFlow(CameraExerciseState(freePractice = planned == null, targetReps = exercise.exeReps.takeIf { it > 0 } ?: 10, targetSets = exercise.exeSets.coerceAtLeast(1)))
    val state = _state.asStateFlow()
    private var remoteCompleted: Collection<Int> = emptyList()
    private var galleryJob: Job? = null
    private var engine: CameraExerciseEngine? = null
    private val countdown = CameraStartCountdown()
    private var clockJob: Job? = null
    private var lastFrameAt = 0L
    private var startedAt = 0L
    val selectedLandmarks get() = state.value.joint?.landmarks(state.value.side) ?: emptyList()

    init {
        load()
        viewModelScope.launch {
            while (true) {
                delay(200)
                val now = SystemClock.elapsedRealtime()
                if (lastFrameAt > 0 && now - lastFrameAt > 400) trackingLost()
                val current = state.value
                val ready = !current.loading && current.error == null && !current.recording && !current.finalizing &&
                    !current.saving && !current.complete && current.pendingVideo == null && now - lastFrameAt <= 400 && engine?.canStart() == true
                val seconds = countdown.update(ready, now)
                _state.update { it.copy(startCountdown = seconds) }
            }
        }
    }

    fun load() {
        if (state.value.recording || state.value.saving) return
        cancelAutoStart()
        _state.update { it.copy(loading = true, error = null, profile = null) }
        viewModelScope.launch {
            try {
                val user = dependencies.sessionManager.userLoggedIn
                check(user.id == owner)
                val service = dependencies.services.exercisesService
                val info = executeRequest { service.getExerciseInfo(exercise.exeID, user.accessToken) }
                require(info.supportsCamera == true) { context.getString(R.string.message_camera_monitoring_is_not_available_for_this_exercise_choose) }
                val joint = CameraJoint.valueOf(requireNotNull(info.cameraJoint))
                val movement = CameraMovement.valueOf(requireNotNull(info.cameraMovement))
                val profile = if (planned == null) info.movementProfile() else executeRequest {
                    service.getExerciseProfile(user.id, planned.planId, planned.dailyListId, planned.exercise.id, user.accessToken)
                }
                require(profile.isValid()) { context.getString(R.string.message_your_physiotherapist_needs_to_review_this_exercise_profile) }
                val done = if (planned == null) emptyList() else executeRequest {
                    service.getSensorProgress(user.id, planned.planId, planned.dailyListId, planned.exercise.id, user.accessToken)
                }.completedSets
                remoteCompleted = done
                migratePendingVideo()
                // Only server acknowledgement clears a pending entry. Original videos remain on device.
                done.forEach { forget(it) }
                val deferred = deferredSets()
                val next = VideoUploadPolicy.nextSet(state.value.targetSets, done, deferred)
                val entry = setKey(next ?: state.value.targetSets)
                val pending = if (next == null) null else preferences.getString(entry, null)?.takeIf { File(it).isFile }
                engine = CameraExerciseEngine(profile, joint, movement, state.value.targetReps)
                _state.update { it.copy(loading = false, info = info, profile = profile, joint = joint, movement = movement,
                    currentSet = next ?: it.targetSets, complete = next == null, pendingVideo = pending, pendingUploads = deferred.size, withLoad = if (pending != null && preferences.contains("${entry}_load")) preferences.getBoolean("${entry}_load", false) else null, loadText = if (pending != null) preferences.getString("${entry}_weight", "").orEmpty() else "", loadSelectionLocked = pending != null && preferences.getBoolean("${entry}_submitted", false), measurement = CameraMeasurement()) }
                if (pending != null) exportGallery()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update { it.copy(loading = false, error = e.message ?: context.getString(R.string.message_unable_to_load_the_exercise_try_again)) }
            }
        }
    }

    fun chooseSide(side: BodySide) {
        if (state.value.recording || state.value.finalizing) return
        cancelAutoStart()
        engine?.invalidate()
        _state.update { it.copy(side = side, measurement = CameraMeasurement()) }
    }
    fun invalidateCalibration() {
        cancelAutoStart()
        engine?.invalidate()
        _state.update { it.copy(measurement = CameraMeasurement()) }
    }
    fun calibrate() {
        if (state.value.saving || state.value.recording || state.value.pendingVideo != null || state.value.finalizing) return
        cancelAutoStart()
        countdown.arm()
        engine?.calibrate()
        _state.update { it.copy(measurement = engine?.measurement ?: CameraMeasurement(), message = null, autoStartPending = true) }
    }
    fun cancelAutoStart() {
        countdown.cancel()
        _state.update { it.copy(autoStartPending = false, startCountdown = null) }
    }
    fun prepareStart() {
        if (state.value.saving || !state.value.measurement.calibrated || state.value.recording || state.value.finalizing) return
        countdown.arm()
        _state.update { it.copy(autoStartPending = true, message = null) }
    }
    fun onPose(pose: Pose, width: Int, height: Int) {
        val points = selectedLandmarks.mapNotNull { pose.getPoseLandmark(it) }.map {
            val position = it.position
            val inFrame = position.x in 0f..width.toFloat() && position.y in 0f..height.toFloat()
            PosePoint(position.x, position.y, if (inFrame) it.inFrameLikelihood else 0f)
        }
        val joint = state.value.joint ?: return
        val raw = if (points.size == 3) CameraAngles.measure(joint, JointObservation(points[0], points[1], points[2])) else null
        lastFrameAt = SystemClock.elapsedRealtime()
        val result = engine?.update(raw, lastFrameAt) ?: return
        if (engine?.canStart() != true) countdown.update(false, SystemClock.elapsedRealtime())
        _state.update { it.copy(measurement = result) }
    }
    fun trackingLost() {
        val result = engine?.update(null, SystemClock.elapsedRealtime()) ?: return
        if (engine?.canStart() != true) countdown.update(false, SystemClock.elapsedRealtime())
        _state.update { it.copy(measurement = result) }
    }
    fun start(): Boolean {
        if (state.value.saving || state.value.loading || state.value.error != null || state.value.pendingVideo != null || state.value.complete || state.value.finalizing) return false
        if (SystemClock.elapsedRealtime() - lastFrameAt > 400 || engine?.start() != true) return false
        cancelAutoStart()
        startedAt = SystemClock.elapsedRealtime()
        _state.update { it.copy(recording = true, elapsedSeconds = 0, message = null, measurement = engine!!.measurement) }
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (true) {
                delay(500)
                _state.update { it.copy(elapsedSeconds = ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()) }
            }
        }
        return true
    }
    fun stopCapture() {
        engine?.stop(); clockJob?.cancel()
        _state.update { it.copy(recording = false, finalizing = true) }
    }
    fun videoSaved(file: File, completed: Boolean) {
        if (completed && file.isFile && file.length() > 0) {
            val persisted = planned == null || preferences.edit().putString(key, file.absolutePath).remove("${key}_load").remove("${key}_weight").remove("${key}_submitted").commit()
            _state.update { it.copy(finalizing = false, pendingVideo = file.absolutePath, withLoad = null, loadText = "", loadSelectionLocked = false,
                message = context.getString(if (persisted) R.string.message_set_complete else R.string.camera_local_save_failed)) }
            exportGallery()
        } else {
            file.delete() // Only this newly-created, incomplete recording, never a pending result.
            _state.update { it.copy(finalizing = false, message = context.getString(R.string.message_set_stopped_calibrate_to_begin_again)) }
            invalidateCalibration()
        }
    }
    fun captureFailed(message: String) {
        engine?.stop(); clockJob?.cancel()
        _state.update { it.copy(recording = false, finalizing = false, message = message) }
        invalidateCalibration()
    }
    fun setWithLoad(value: Boolean) {
        if (state.value.saving || state.value.loadSelectionLocked || state.value.pendingVideo == null) return
        preferences.edit().putBoolean("${key}_load", value).apply { if (!value) remove("${key}_weight") }.commit()
        _state.update { it.copy(withLoad = value, loadText = if (value) it.loadText else "") }
    }
    fun setLoadText(text: String) {
        if (state.value.saving || state.value.loadSelectionLocked || state.value.pendingVideo == null || state.value.withLoad != true) return
        preferences.edit().putString("${key}_weight", text).commit()
        _state.update { it.copy(loadText = text) }
    }
    fun exportGallery() {
        val path = state.value.pendingVideo ?: return
        if (galleryJob?.isActive == true) return
        if (!CameraGallery.permitted(context)) {
            _state.update { it.copy(galleryMessage = context.getString(R.string.camera_gallery_permission)) }
            return
        }
        galleryJob = viewModelScope.launch {
            try {
                CameraGallery.save(context, File(path))
                _state.update { it.copy(galleryMessage = context.getString(R.string.camera_gallery_saved)) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _state.update { it.copy(galleryMessage = context.getString(R.string.camera_gallery_failed)) }
            }
        }
    }

    private fun persistCurrent(): Boolean {
        val current = state.value
        val path = current.pendingVideo ?: return false
        val loaded = current.withLoad ?: return false
        val saved = preferences.edit().putString(key, path).putBoolean("${key}_load", loaded)
            .putString("${key}_weight", current.loadText).commit()
        if (!saved) _state.update { it.copy(message = context.getString(R.string.camera_local_save_failed)) }
        return saved
    }

    fun saveLocally() {
        if (planned == null || state.value.saving || !pt.ipc_app.ui.components.validLoadSelection(state.value.withLoad, state.value.loadText)) return
        if (!persistCurrent()) return
        if (!defer(state.value.currentSet)) {
            _state.update { it.copy(message = context.getString(R.string.camera_local_save_failed)) }
            return
        }
        cancelAutoStart()
        val current = state.value
        engine = CameraExerciseEngine(requireNotNull(current.profile), requireNotNull(current.joint), requireNotNull(current.movement), current.targetReps)
        val next = VideoUploadPolicy.nextSet(current.targetSets, remoteCompleted, deferredSets())
        // Continuing a recorded series must not depend on another network request.
        _state.update { it.copy(pendingVideo = null, pendingUploads = deferredSets().size,
            currentSet = next ?: it.targetSets, complete = next == null, withLoad = null, loadText = "",
            loadSelectionLocked = false, measurement = CameraMeasurement(),
            message = context.getString(R.string.camera_saved_pending)) }
    }

    fun save() {
        if (state.value.pendingVideo == null || state.value.saving || !pt.ipc_app.ui.components.validLoadSelection(state.value.withLoad, state.value.loadText)) return
        if (planned == null) {
            // Free-practice videos remain on the phone too.
            _state.update { it.copy(pendingVideo = null, complete = it.currentSet >= it.targetSets, currentSet = (it.currentSet + 1).coerceAtMost(it.targetSets), message = context.getString(R.string.message_free_practice_complete)) }
            invalidateCalibration()
            return
        }
        if (!persistCurrent()) return
        uploadSets(listOf(state.value.currentSet))
    }

    fun retryPendingUploads() {
        if (state.value.saving || state.value.recording || state.value.finalizing) return
        uploadSets(deferredSets().sorted())
    }

    private fun uploadSets(sets: List<Int>) {
        val prescription = planned ?: return
        if (sets.isEmpty()) return
        cancelAutoStart()
        _state.update { it.copy(saving = true, message = context.getString(R.string.camera_preparing_upload)) }
        viewModelScope.launch {
            try {
                for (set in sets) {
                    val entry = setKey(set)
                    val path = preferences.getString(entry, null) ?: continue
                    val user = dependencies.sessionManager.userLoggedIn
                    check(user.id == owner) { context.getString(R.string.message_sign_in_again_with_the_account_used_to_perform_this_exercise) }
                    val service = dependencies.services.exercisesService
                    val done = executeRequest { service.getSensorProgress(user.id, prescription.planId, prescription.dailyListId, prescription.exercise.id, user.accessToken) }.completedSets
                    if (set !in done) {
                        val original = File(path)
                        val upload = CameraVideoUpload(context).prepare(original)
                        try {
                            check(dependencies.sessionManager.userLoggedIn.id == owner)
                            // Once sent, preserve metadata across ambiguous failures/lost acknowledgements.
                            check(preferences.edit().putBoolean("${entry}_submitted", true).commit())
                            if (set == state.value.currentSet) _state.update { it.copy(loadSelectionLocked = true) }
                            val loaded = preferences.getBoolean("${entry}_load", false)
                            val weight = if (loaded) pt.ipc_app.ui.components.parseLoadKg(preferences.getString("${entry}_weight", "").orEmpty()) else null
                            executeRequest {
                                service.submitExerciseVideo(upload, java.util.UUID.fromString(owner), prescription.planId, prescription.dailyListId, prescription.exercise.id, set, dependencies.sessionManager.userLoggedIn.accessToken, loaded, weight)
                            }
                        } finally {
                            if (upload != original) upload.delete()
                        }
                    }
                    forget(set)
                    remoteCompleted = remoteCompleted + set
                    if (set == state.value.currentSet) _state.update { it.copy(pendingVideo = null) }
                    _state.update { it.copy(pendingUploads = deferredSets().size) }
                }
                _state.update { it.copy(saving = false, message = context.getString(R.string.message_set_saved_rest_before_continuing)) }
                load()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val tooLarge = e is VideoTooLargeException || (e is pt.ipc_app.service.connection.UnexpectedResponseException && e.statusCode == 413)
                _state.update { it.copy(saving = false, message = context.getString(if (tooLarge) R.string.camera_upload_too_large else R.string.camera_upload_failed)) }
            }
        }
    }
}
