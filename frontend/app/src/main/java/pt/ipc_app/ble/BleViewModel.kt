package pt.ipc_app.ble

import pt.ipc_app.R

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import android.os.SystemClock
import android.util.Log
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.domain.exercise.Exercise
import pt.ipc_app.domain.exercise.ExerciseTotalInfo
import pt.ipc_app.service.models.exercises.SensorProfile
import pt.ipc_app.service.models.exercises.SensorSession
import pt.ipc_app.service.models.exercises.SensorSample
import pt.ipc_app.utils.executeRequest
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import pt.ipc_app.ui.screens.AppViewModel

class BleViewModel(
    private val appContext: Context
) : AppViewModel() {

    private val _uiState = MutableStateFlow(BleUiState())
    val uiState: StateFlow<BleUiState> = _uiState.asStateFlow()

    private val rollHistoryMax = 150
    private val dependencies get() = appContext.applicationContext as DependenciesContainer
    private var exercise: Exercise? = null
    private var completedSets = emptyList<Int>()
    private var startedAt = 0L
    private val samples = mutableListOf<SensorSample>()
    private var pending: SensorSession? = null
    private var previousReps = 0
    private var calibrationAttempt = 0
    private var sentProfileCommand: String? = null
    private var acceptedProfileCommand: String? = null
    private var startAttempt = 0
    private val pendingPreferences = appContext.getSharedPreferences("pending_sensor_sessions", Context.MODE_PRIVATE)
    private fun pendingKey(): String? = (exercise as? ExerciseTotalInfo)?.let {
        "${dependencies.sessionManager.userUUID}_${it.planId}_${it.dailyListId}_${it.exercise.id}"
    }

    private fun targetRepetitions(): Int =
        exercise?.exeReps?.takeIf { it > 0 } ?: 10

    private fun calibrationCommand(profile: SensorProfile): String? = try {
        exercise?.let { SensorExerciseCommand.calibration(it.exeTitle, profile, targetRepetitions()) }
    } catch (_: IllegalArgumentException) { null }

    fun configureExercise(value: Exercise) {
        if (exercise != null) return
        exercise = value
        reloadProfile()
    }

    fun reloadProfile() {
        val planned = exercise as? ExerciseTotalInfo
        if (_uiState.value.sessionRunning || _uiState.value.sessionStarting || _uiState.value.sensorCalibrating) return
        sentProfileCommand = null; acceptedProfileCommand = null
        _uiState.update { it.copy(profileLoading = true, sensorCalibrated = false, lastError = null) }
        viewModelScope.launch {
            try {
                val user = dependencies.sessionManager.userLoggedIn
                val service = dependencies.services.exercisesService
                if (planned == null) {
                    val selected = exercise ?: return@launch
                    val info = executeRequest { service.getExerciseInfo(selected.exeID, user.accessToken) }
                    _uiState.update { it.copy(profile = info.sensorProfile(), sessionMessage = appContext.getString(R.string.free_practice_profile, info.title)) }
                    return@launch
                }
                val profile = executeRequest { service.getSensorProfile(user.id, planned.planId, planned.dailyListId, planned.exercise.id, user.accessToken) }
                completedSets = executeRequest { service.getSensorProgress(user.id, planned.planId, planned.dailyListId, planned.exercise.id, user.accessToken) }.completedSets
                val saved = pendingPreferences.getString(pendingKey(), null)
                pending = saved?.let { dependencies.jsonEncoder.fromJson(it, SensorSession::class.java) }
                val next = (1..planned.exeSets).firstOrNull { it !in completedSets }
                if (pending?.set in completedSets) {
                    pending = null
                    pendingPreferences.edit().remove(pendingKey()).apply()
                }
                _uiState.update { it.copy(profile = profile, currentSet = pending?.set ?: next ?: planned.exeSets,
                    exerciseComplete = next == null, sessionReadyToSave = pending != null, withLoad = pending?.withLoad, loadText = pending?.loadValue?.toString().orEmpty(), loadSelectionLocked = pending != null && pendingPreferences.getBoolean("${pendingKey()}_submitted", false)) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(lastError = e.message ?: appContext.getString(R.string.feedback_unable_to_load_the_sensor_profile)) }
            } finally { _uiState.update { it.copy(profileLoading = false) } }
        }
    }

    fun setMounting(useRoll: Boolean, direction: Int) {
        val state = _uiState.value
        if (state.sessionRunning || state.sessionStarting || state.sensorCalibrating || state.sessionReadyToSave) return
        val profile = state.profile ?: return
        if (direction != 1 && direction != -1) return
        val adjusted = if (profile.useRoll == useRoll) profile else profile.copy(useRoll = useRoll,
            minPitch = profile.minRoll, maxPitch = profile.maxRoll,
            minRoll = profile.minPitch, maxRoll = profile.maxPitch)
        sentProfileCommand = null; acceptedProfileCommand = null
        _uiState.update { it.copy(profile = adjusted.copy(movementDirection = direction), sensorCalibrated = false,
            sessionMessage = appContext.getString(R.string.feedback_placement_updated_for_this_session_return_to_the_starting_positio)) }
    }

    fun startSet() {
        val state = _uiState.value
        if (state.profile == null) return
        if (acceptedProfileCommand == null || acceptedProfileCommand != calibrationCommand(state.profile)) {
            _uiState.update { it.copy(sensorCalibrated = false, lastError = appContext.getString(R.string.feedback_the_exercise_changed_calibrate_again_to_send_the_current_profile)) }; return
        }
        if (!state.servicesDiscovered || !state.sensorCalibrated || state.sensorCalibrating ||
            state.sessionRunning || state.sessionStarting || state.sessionReadyToSave ||
            state.exerciseComplete
        ) return

        samples.clear()
        previousReps = 0
        _uiState.update {
            it.copy(
                sessionStarting = true,
                lastError = null,
                sensorReps = 0,
                rollHistory = emptyList()
            )
        }

        // The exercise profile was already transferred during CALIBRATE2.
        // START2 only tells the ESP32-C3 to start the calibrated session.
        val attempt = ++startAttempt
        bleManager.writeText("START2")

        viewModelScope.launch {
            delay(5000)
            if (attempt == startAttempt && _uiState.value.sessionStarting) {
                bleManager.writeText("STOP")
                _uiState.update {
                    it.copy(
                        sessionStarting = false,
                        lastError = appContext.getString(R.string.feedback_the_sensor_did_not_confirm_the_start_reconnect_and_try_again)
                    )
                }
            }
        }
    }

    fun calibrate() {
        val state = _uiState.value
        val profile = state.profile ?: return

        if (!state.servicesDiscovered || state.sensorCalibrating ||
            state.sessionRunning || state.sessionStarting
        ) return

        val command = calibrationCommand(profile)
        if (command == null) {
            _uiState.update {
                it.copy(
                    sensorCalibrating = false,
                    sensorCalibrated = false,
                    lastError = appContext.getString(R.string.feedback_unable_to_create_the_exercise_ble_profile)
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                sensorCalibrating = true,
                sensorCalibrated = false,
                lastError = null,
                sessionMessage = appContext.getString(R.string.feedback_keep_the_sensors_still_in_the_neutral_position)
            )
        }

        // Sends:
        // CALIBRATE2:exerciseName,reps,raise,lower,minRaiseMs,holdMs,cooldownMs,
        //            minSpeed,minPitch,maxPitch,minRoll,maxRoll,useRoll,direction
        sentProfileCommand = command
        acceptedProfileCommand = null
        bleManager.writeText(command)
        watchCalibrationTimeout()
    }

    private fun watchCalibrationTimeout() {
        val attempt = ++calibrationAttempt
        viewModelScope.launch {
            // Calibration on the wearable is asynchronous. Do not send STOP merely
            delay(15000)

            if (attempt == calibrationAttempt && _uiState.value.sensorCalibrating) {
                _uiState.update {
                    it.copy(
                        sensorCalibrating = false,
                        sensorCalibrated = false,
                        lastError = appContext.getString(R.string.feedback_calibration_is_taking_longer_than_expected_wait_for_sensor_confir),
                        sessionMessage = appContext.getString(R.string.feedback_waiting_for_calibration_confirmation)
                    )
                }
            }
        }
    }

    fun setWithLoad(value: Boolean) {
        if (_uiState.value.sessionSaving || _uiState.value.loadSelectionLocked) return
        val result = pending ?: return
        pending = result.copy(withLoad = value, loadValue = if (value) result.loadValue else null, loadUnit = if (value) result.loadUnit else null)
        pendingKey()?.let { pendingPreferences.edit().putString(it, dependencies.jsonEncoder.toJson(pending)).commit() }
        _uiState.update { it.copy(withLoad = value, loadText = if (value) it.loadText else "") }
    }

    fun setLoadText(text: String) {
        if (_uiState.value.sessionSaving || _uiState.value.loadSelectionLocked || _uiState.value.withLoad != true) return
        val result = pending ?: return
        val weight = pt.ipc_app.ui.components.parseLoadKg(text)
        pending = result.copy(loadValue = weight, loadUnit = if (weight != null) "kg" else null)
        pendingKey()?.let { pendingPreferences.edit().putString(it, dependencies.jsonEncoder.toJson(pending)).commit() }
        _uiState.update { it.copy(loadText = text) }
    }

    fun saveSet() {
        val result = pending ?: return
        val planned = exercise as? ExerciseTotalInfo
        if (planned == null) {
            pending = null
            _uiState.update { it.copy(sessionReadyToSave = false, exerciseComplete = true, sessionMessage = appContext.getString(R.string.feedback_free_practice_complete_the_result_was_not_submitted)) }
            return
        }
        if (_uiState.value.sessionSaving || !pt.ipc_app.ui.components.validLoadSelection(result.withLoad, _uiState.value.loadText)) return
        pendingPreferences.edit().putBoolean("${pendingKey()}_submitted", true).commit()
        _uiState.update { it.copy(sessionSaving = true, loadSelectionLocked = true, lastError = null) }
        viewModelScope.launch {
            try {
                val user = dependencies.sessionManager.userLoggedIn
                executeRequest { dependencies.services.exercisesService.submitSensorSession(UUID.fromString(user.id), planned.planId,
                    planned.dailyListId, planned.exercise.id, result, user.accessToken) }
                completedSets = completedSets + result.set
                pendingPreferences.edit().remove(pendingKey()).remove("${pendingKey()}_submitted").commit()
                pending = null
                val next = (1..planned.exeSets).firstOrNull { it !in completedSets }
                _uiState.update { it.copy(sessionReadyToSave = false, withLoad = null, loadText = "", loadSelectionLocked = false, exerciseComplete = next == null,
                    currentSet = next ?: planned.exeSets, sessionMessage = appContext.getString(R.string.feedback_set_saved_you_can_rest_before_the_next_set)) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update { it.copy(lastError = e.message ?: appContext.getString(R.string.feedback_submission_failed_the_result_is_saved_on_this_device_try_again)) }
            } finally { _uiState.update { it.copy(sessionSaving = false) } }
        }
    }

    private fun recordSample(parsed: ParsedSensorLine) {
        if (!_uiState.value.sessionRunning) return
        if (parsed.reps < previousReps) {
            _uiState.update { it.copy(sessionRunning = false, lastError = appContext.getString(R.string.feedback_the_sensor_counter_was_reset_start_a_new_set)) }
            return
        }
        previousReps = parsed.reps
        val elapsed = SystemClock.elapsedRealtime() - startedAt
        val target = targetRepetitions()
        if (samples.isEmpty() || elapsed - samples.last().elapsedMs >= 200 || parsed.reps >= target) {
            if (samples.size >= 2000) {
                val reduced = samples.filterIndexed { index, _ -> index % 2 == 0 }
                samples.clear(); samples.addAll(reduced)
            }
            samples.add(SensorSample(elapsed, parsed.pitch, parsed.roll, parsed.velocity))
        }
        if (parsed.reps == target && parsed.status == "SET COMPLETE") {
            pending = SensorSession(UUID.randomUUID(), _uiState.value.currentSet, target, elapsed.coerceAtLeast(1), samples.toList(), _uiState.value.profile)
            pendingKey()?.let { pendingPreferences.edit().putString(it, dependencies.jsonEncoder.toJson(pending)).commit() }
            _uiState.update { it.copy(sessionRunning = false, sessionReadyToSave = true, sessionMessage = appContext.getString(R.string.feedback_set_complete_save_the_result_to_continue)) }
        }
    }

    private val bleManager = BleManager(
        appContext = appContext,
        scope = viewModelScope,
        onEvent = ::handleEvent
    )

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun refreshCapabilitiesAndPermissions() {
        val bleSupported = bleManager.isBleSupported()
        val granted = requiredPermissions().all { p ->
            ContextCompat.checkSelfPermission(appContext, p) == PackageManager.PERMISSION_GRANTED
        }
        val bluetoothEnabled = granted && bleManager.isBluetoothEnabled()
        _uiState.update {
            it.copy(
                bleSupported = bleSupported,
                bluetoothEnabled = bluetoothEnabled,
                permissionsGranted = granted,
                lastError = null
            )
        }
    }

    fun startScan() {
        refreshCapabilitiesAndPermissions()
        val s = _uiState.value
        if (!s.bleSupported) {
            _uiState.update { it.copy(lastError = appContext.getString(R.string.message_this_device_does_not_support_ble)) }
            return
        }
        if (!s.bluetoothEnabled) {
            _uiState.update { it.copy(lastError = appContext.getString(R.string.message_bluetooth_is_disabled)) }
            return
        }
        if (!s.permissionsGranted) {
            _uiState.update { it.copy(lastError = appContext.getString(R.string.message_bluetooth_permission_is_required)) }
            return
        }
        bleManager.startScan()
    }

    fun stopScan() = bleManager.stopScan()

    fun connect() {
        val device = _uiState.value.device ?: run {
            _uiState.update { it.copy(lastError = appContext.getString(R.string.message_select_a_device)) }
            return
        }
        refreshCapabilitiesAndPermissions()
        if (!_uiState.value.permissionsGranted) {
            _uiState.update { it.copy(lastError = appContext.getString(R.string.message_bluetooth_permission_is_required)) }
            return
        }
        bleManager.connect(device)
    }

    fun disconnect() {
        bleManager.disconnect()
        _uiState.update {
            it.copy(
                connecting = false,
                connected = false,
                sessionRunning = false,
                sessionStarting = false,
                sensorCalibrating = false,
                sensorCalibrated = false,
                servicesDiscovered = false,
                receivedValue = "",
                sensorAngle = null,
                sensorPitch = null,
                sensorRoll = null,
                sensorReps = null,
                sensorVelocity = null,
                sensorStatus = "",
                rollHistory = emptyList()
            )
        }
    }

    fun clearSensorHistory() {
        _uiState.update { it.copy(rollHistory = emptyList()) }
    }

    fun read() = bleManager.read()

    fun writeText(text: String) = bleManager.writeText(text)

    override fun onCleared() {
        bleManager.disconnect()
        super.onCleared()
    }

    private fun handleEvent(event: BleManager.BleEvent) {
        if (event is BleManager.BleEvent.Disconnected || event is BleManager.BleEvent.Connecting) {
            sentProfileCommand = null; acceptedProfileCommand = null; startAttempt++
        }
        when (event) {
            is BleManager.BleEvent.Error ->
                _uiState.update { it.copy(lastError = event.message, scanning = false, connecting = false, sensorCalibrated = false, sensorCalibrating = false, sessionStarting = false) }

            is BleManager.BleEvent.ScanStarted ->
                _uiState.update { it.copy(scanning = true, lastError = null, device = null) }

            BleManager.BleEvent.ScanStopped ->
                _uiState.update { it.copy(scanning = false) }

            is BleManager.BleEvent.DeviceFound ->
                _uiState.update { it.copy(device = event.device, scanning = false, lastError = null) }

            BleManager.BleEvent.Connecting ->
                _uiState.update { it.copy(connecting = true, connected = false, servicesDiscovered = false, lastError = null) }

            BleManager.BleEvent.Connected ->
                _uiState.update { it.copy(connecting = false, connected = true, sensorCalibrated = false, lastError = null) }

            BleManager.BleEvent.Disconnected ->
                _uiState.update {
                    it.copy(
                        connecting = false,
                        connected = false,
                        sessionRunning = false,
                        sessionStarting = false,
                        sensorCalibrating = false,
                        sensorCalibrated = false,
                        servicesDiscovered = false,
                        receivedValue = "",
                        sensorAngle = null,
                sensorPitch = null,
                        sensorRoll = null,
                        sensorReps = null,
                        sensorVelocity = null,
                        sensorStatus = "",
                        rollHistory = emptyList()
                    )
                }

            BleManager.BleEvent.ServicesDiscovered ->
                _uiState.update { it.copy(servicesDiscovered = true, lastError = null) }

            is BleManager.BleEvent.ValueReceived -> {
                val message = event.value.trim()
                Log.d("BLE_DEBUG", "RX: [$message]")
                if (message == "SESSION STARTED" && _uiState.value.sessionStarting) {
                    startedAt = SystemClock.elapsedRealtime()
                    _uiState.update { it.copy(sessionStarting = false, sessionRunning = true, sessionMessage = appContext.getString(R.string.feedback_set_in_progress)) }
                } else if (message == "CALIBRATION COMPLETE") {
                    // The ESP32 only sends CALIBRATION COMPLETE after it has parsed and
                    // accepted the CALIBRATE2 profile and finished collecting the samples.
                    // Therefore PROFILE ACCEPTED is useful feedback, but it must not be a
                    // mandatory prerequisite: consecutive BLE notifications can occasionally
                    // arrive late or one acknowledgement may be missed by the Android side.
                    calibrationAttempt++

                    val matchesPendingProfile = sentProfileCommand != null &&
                        _uiState.value.profile?.let { calibrationCommand(it) } == sentProfileCommand
                    if (!matchesPendingProfile) return
                    acceptedProfileCommand = sentProfileCommand

                    _uiState.update {
                        it.copy(
                            sensorCalibrating = false,
                            sensorCalibrated = true,
                            lastError = null,
                            sessionMessage = appContext.getString(R.string.feedback_calibration_complete_you_can_start_the_set)
                        )
                    }
                } else if (message == "PROFILE ACCEPTED" && sentProfileCommand != null && _uiState.value.profile?.let { calibrationCommand(it) } == sentProfileCommand) {
                    acceptedProfileCommand = sentProfileCommand
                    _uiState.update { it.copy(sensorCalibrating = true, sensorCalibrated = false, sessionMessage = appContext.getString(R.string.feedback_exercise_received_by_the_wearable_calibrating)) }
                } else if (message == "CALIBRATION REQUIRED" && !_uiState.value.sensorCalibrating) {
                    _uiState.update { it.copy(sensorCalibrated = false, sensorCalibrating = false, sessionMessage = appContext.getString(R.string.feedback_calibrate_the_sensors_in_the_starting_position_before_beginning)) }
                } else if (message == "SESSION RESET" || message == "CALIBRATING" || message.startsWith("ERROR:")) {
                    if (message == "SESSION RESET" || message.startsWith("ERROR:")) {
                        calibrationAttempt++
                    }

                    val explanation = when (message) {
                        "ERROR:CALIBRATION MOVED" -> appContext.getString(R.string.feedback_the_sensors_moved_during_calibration_keep_them_still_and_calibrat)
                        "ERROR:SENSOR MISSING" -> appContext.getString(R.string.feedback_check_both_sensors_and_their_cables_then_calibrate_again)
                        "ERROR:INVALID SENSOR DATA" -> appContext.getString(R.string.feedback_check_sensor_placement_the_selected_axis_must_follow_movement_in)
                        "ERROR:PROFILE REQUIRED" -> appContext.getString(R.string.feedback_load_the_exercise_and_calibrate_through_the_app_before_using_the)
                        "ERROR:NOT READY", "ERROR:CALIBRATION REQUIRED" -> appContext.getString(R.string.feedback_the_sensor_is_not_ready_check_the_connection_and_calibrate_again)
                        "SESSION RESET" -> appContext.getString(R.string.feedback_set_stopped_by_the_sensor)
                        "CALIBRATING" -> appContext.getString(R.string.feedback_keep_the_sensors_still_in_the_neutral_position)
                        else -> appContext.getString(R.string.feedback_the_sensor_rejected_the_command_check_the_profile_and_calibrate_a)
                    }
                    _uiState.update { it.copy(sessionStarting = false, sessionRunning = false,
                        sensorCalibrating = message == "CALIBRATING",
                        sensorCalibrated = if (message == "SESSION RESET") it.sensorCalibrated else false,
                        lastError = if (message.startsWith("ERROR:")) explanation else null,
                        sessionMessage = explanation) }
                    if (message == "CALIBRATING") watchCalibrationTimeout()
                }
                val parsed = SensorBleParser.parse(event.value)
                if (parsed != null) recordSample(parsed)
                _uiState.update { state ->
                    if (parsed != null) {
                        val nextHistory =
                            (state.rollHistory + parsed.angle).takeLast(rollHistoryMax)
                        state.copy(
                            receivedValue = event.value,
                            sensorAngle = parsed.angle,
                            sensorPitch = parsed.pitch,
                            sensorRoll = parsed.roll,
                            sensorReps = parsed.reps,
                            sensorVelocity = parsed.velocity,
                            sensorStatus = parsed.status,
                            rollHistory = nextHistory
                        )
                    } else {
                        state.copy(receivedValue = event.value)
                    }
                }
            }
        }
    }
}
