package pt.ipc_app.ui.screens.exercises.sensor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Warning
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.ble.BleUiState
import pt.ipc_app.ble.BleViewModel
import pt.ipc_app.ui.components.ScreenHeader
import pt.ipc_app.ui.theme.*
import java.util.Locale
import pt.ipc_app.feedback.ExerciseVoiceFeedback
import pt.ipc_app.feedback.VoiceFeedbackControl

@Composable
fun SensorScreen(
    voice: ExerciseVoiceFeedback,
    viewModel: BleViewModel,
    exerciseTitle: String,
    exerciseDescription: String,
    targetReps: Int,
    targetSets: Int,
    rollRaiseTargetDeg: Float,
    rollLowerTargetDeg: Float,
    onExit: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var executionScreen by rememberSaveable { mutableStateOf(false) }
    var preparationStep by rememberSaveable { mutableStateOf(0) }
    var advancedMounting by rememberSaveable { mutableStateOf(false) }
    val ready = uiState.servicesDiscovered && uiState.sensorCalibrated && !uiState.sensorCalibrating && uiState.profile != null
    val active = uiState.sessionRunning || uiState.sessionStarting
    LaunchedEffect(ready, uiState.sessionReadyToSave, uiState.exerciseComplete) {
        if (!ready && executionScreen && !uiState.sessionReadyToSave && !uiState.exerciseComplete) {
            executionScreen = false
            preparationStep = if (uiState.servicesDiscovered) 2 else 1
        }
    }
    BackHandler(enabled = executionScreen && !active && !uiState.sessionSaving) { executionScreen = false }
    // Keep a hardware series alive only while its execution screen is visible.
    BackHandler(enabled = executionScreen && active) {
        viewModel.writeText("STOP")
        executionScreen = false
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshCapabilitiesAndPermissions()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshCapabilitiesAndPermissions()
    }

    val needsBleBanner = !uiState.bleSupported ||
        !uiState.bluetoothEnabled ||
        !uiState.permissionsGranted ||
        !uiState.lastError.isNullOrBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        ScreenHeader(
            title = exerciseTitle.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            },
            subtitle = if (executionScreen) stringResource(R.string.ui_2_of_2_perform_exercise) else stringResource(R.string.ui_1_of_2_prepare_sensors),
            titleAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VoiceFeedbackControl(voice)
            if (needsBleBanner) {
                BluetoothIssueBanner(
                    state = uiState,
                    onGrantPermissions = {
                        permissionsLauncher.launch(viewModel.requiredPermissions())
                    }
                )
            }

            if (uiState.profileLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (uiState.profile == null && !uiState.profileLoading) {
                Text(stringResource(R.string.ui_your_physiotherapist_needs_to_configure_the_limits_for_this_exerc))
                TextButton(onClick = viewModel::reloadProfile) { Text(stringResource(R.string.ui_refresh_profile)) }
            }
            if (!executionScreen) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(stringResource(R.string.ui_position), stringResource(R.string.ui_connect), stringResource(R.string.ui_calibrate)).forEachIndexed { index, label ->
                        OutlinedButton(onClick = { preparationStep = index }, enabled = index < 2 || uiState.servicesDiscovered,
                            modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (index == preparationStep) SurfaceLight else CardBackground)) {
                            Text("${index + 1} · $label", style = MaterialTheme.typography.caption)
                        }
                    }
                }
                if (uiState.sessionReadyToSave || uiState.exerciseComplete) {
                    Button(onClick = { executionScreen = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_view_result)) }
                }
                if (preparationStep == 0) {
                    SensorPlacementCard(exerciseTitle, exerciseDescription)
                    Button(onClick = { preparationStep = 1 }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_sensors_in_position_continue)) }
                }
                if (preparationStep == 1) {
                    Text(stringResource(R.string.ui_connect_the_main_module), style = MaterialTheme.typography.h6)
                    Text(stringResource(R.string.ui_the_smaller_sensor_connects_through_the_cable), style = MaterialTheme.typography.body2)
                    CompactDeviceRow(state = uiState, onScanClick = viewModel::startScan, onStopScanClick = viewModel::stopScan,
                        onConnectClick = viewModel::connect, onDisconnectClick = viewModel::disconnect)
                    Text(if (uiState.servicesDiscovered) stringResource(R.string.ui_sensors_connected) else if (uiState.connecting || uiState.connected) stringResource(R.string.ui_connecting) else stringResource(R.string.ui_look_for_esp32_c3_dual_imu), color = MediumBlue)
                    Button(onClick = { preparationStep = 2 }, enabled = uiState.servicesDiscovered, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_continue_to_calibration)) }
                }
                if (preparationStep == 2) {
                Text(stringResource(R.string.ui_adopt_the_starting_position), style = MaterialTheme.typography.h6)
                Text(sensorStartPosition(exerciseTitle)?.let { stringResource(it) } ?: stringResource(R.string.ui_follow_the_starting_position_prescribed_by_your_physiotherapist))
                uiState.profile?.let { profile ->
                    TextButton(onClick = { advancedMounting = !advancedMounting }) { Text(stringResource(R.string.ui_adjust_sensor_orientation)) }
                    if (advancedMounting) {
                        Text(stringResource(R.string.ui_the_angle_should_increase_during_movement_adjustments_require_rec), style = MaterialTheme.typography.caption)
                        val editable = !active && !uiState.sensorCalibrating && !uiState.sessionReadyToSave
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.ui_use_roll_axis_off_pitch), Modifier.weight(1f))
                            Switch(profile.useRoll, enabled = editable,
                                onCheckedChange = { viewModel.setMounting(it, profile.movementDirection) })
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.ui_reverse_direction), Modifier.weight(1f))
                            Switch(profile.movementDirection == -1, enabled = editable,
                                onCheckedChange = { viewModel.setMounting(profile.useRoll, if (it) -1 else 1) })
                        }
                    }
                }
                if (uiState.sensorCalibrated) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.ui_check_movement), style = MaterialTheme.typography.subtitle1)
                            Text(stringResource(R.string.sensor_axis_readings, uiState.sensorPitch?.let(::displaySensorNumber) ?: "—", uiState.sensorRoll?.let(::displaySensorNumber) ?: "—"))
                            uiState.profile?.let { profile ->
                                val angle = uiState.sensorAngle
                                Text(stringResource(R.string.exercise_angle_reading, angle?.let { String.format(java.util.Locale.getDefault(), "%.0f°", it) } ?: "—"))
                            }
                            Text(stringResource(R.string.ui_perform_the_movement_without_twisting_your_limb_the_exercise_angl), style = MaterialTheme.typography.caption)
                        }
                    }
                }
                Text(stringResource(R.string.ui_keep_still_for_3_seconds), color = MediumGrey)
                if (uiState.sensorCalibrating) LinearProgressIndicator(Modifier.fillMaxWidth())
                Button(onClick = viewModel::calibrate, modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.profile != null && uiState.servicesDiscovered && !uiState.sensorCalibrating && !active && !uiState.sessionReadyToSave) {
                    Text(if (uiState.sensorCalibrating) stringResource(R.string.ui_calibrating_hold_your_position) else if (uiState.sensorCalibrated) stringResource(R.string.ui_calibrate_again) else stringResource(R.string.ui_calibrate_starting_position))
                }
                if (ready) {
                    Text(stringResource(R.string.ui_calibration_complete), color = SuccessGreen, fontWeight = FontWeight.Bold)
                    val angle = uiState.sensorAngle
                    Text(stringResource(R.string.calibrated_angle_reading, angle?.let { displaySensorNumber(it) + "°" } ?: "—"))

                }
                Button(onClick = { executionScreen = true }, modifier = Modifier.fillMaxWidth(),
                    enabled = ready || uiState.sessionReadyToSave || uiState.exerciseComplete) {
                    Text(if (uiState.sessionReadyToSave) stringResource(R.string.ui_view_unsaved_result) else stringResource(R.string.ui_continue_to_exercise))
                }
                }
            } else {
                TextButton(onClick = { executionScreen = false; preparationStep = 2 }, enabled = !active && !uiState.sessionSaving) { Text(stringResource(R.string.ui_placement_and_calibration)) }
                LiveExerciseCard(
                    state = uiState, targetReps = targetReps, targetSets = targetSets,
                    rollRaiseTargetDeg = uiState.profile?.raiseThreshold ?: rollRaiseTargetDeg,
                    rollLowerTargetDeg = uiState.profile?.lowerThreshold ?: rollLowerTargetDeg,
                    onClearChart = viewModel::clearSensorHistory
                )
                if (!active && uiState.sessionMessage.isNotBlank()) Text(uiState.sessionMessage)
                if (uiState.exerciseComplete) {
                    Text(stringResource(R.string.ui_exercise_complete))
                    Button(onClick = onExit) { Text(stringResource(R.string.ui_finish)) }
                } else {
                    Button(onClick = viewModel::startSet, modifier = Modifier.fillMaxWidth(),
                        enabled = ready && !active && !uiState.sessionReadyToSave && !uiState.sessionSaving) {
                        Text(if (uiState.sessionStarting) stringResource(R.string.ui_starting) else if (uiState.sessionRunning) stringResource(R.string.ui_set_in_progress) else stringResource(R.string.start_set_number, uiState.currentSet))
                    }
                    if (active) TextButton(onClick = { viewModel.writeText("STOP") }) { Text(stringResource(R.string.ui_stop_set)) }
                    if (uiState.sessionReadyToSave) {
                        pt.ipc_app.ui.components.LoadSelection(uiState.withLoad, !uiState.sessionSaving && !uiState.loadSelectionLocked, viewModel::setWithLoad, uiState.loadText, viewModel::setLoadText)
                        Button(onClick = viewModel::saveSet, enabled = !uiState.sessionSaving && pt.ipc_app.ui.components.validLoadSelection(uiState.withLoad, uiState.loadText), modifier = Modifier.fillMaxWidth()) {
                            Text(if (uiState.sessionSaving) stringResource(R.string.ui_saving) else stringResource(R.string.ui_save_set))
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun BluetoothIssueBanner(
    state: BleUiState,
    onGrantPermissions: () -> Unit
) {
    val message = when {
        !state.bleSupported -> stringResource(R.string.ble_issue_not_supported)
        !state.permissionsGranted -> stringResource(R.string.ble_issue_need_permissions)
        !state.bluetoothEnabled -> stringResource(R.string.ble_issue_enable_bt)
        !state.lastError.isNullOrBlank() -> state.lastError!!
        else -> return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 0.dp,
        shape = CardShape,
        backgroundColor = Color(0xFFFFEBEE)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.body2,
                    color = DarkGrey
                )
                if (!state.permissionsGranted && state.bleSupported) {
                    TextButton(onClick = onGrantPermissions) {
                        Text(
                            text = stringResource(R.string.ble_grant_permissions),
                            color = MediumBlue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactDeviceRow(
    state: BleUiState,
    onScanClick: () -> Unit,
    onStopScanClick: () -> Unit,
    onConnectClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 1.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = SurfaceLight
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (state.scanning) Icons.Default.BluetoothSearching else Icons.Default.Bluetooth,
                contentDescription = null,
                tint = MediumBlue,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.device?.name
                        ?: stringResource(R.string.ble_not_found_yet),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Medium,
                    color = DarkGrey,
                    maxLines = 1
                )
            }
            TextButton(
                onClick = if (state.scanning) onStopScanClick else onScanClick,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text(
                    if (state.scanning) stringResource(R.string.ble_stop_scan)
                    else stringResource(R.string.ble_scan),
                    style = MaterialTheme.typography.caption
                )
            }
            Button(
                onClick = if (state.connected || state.connecting) onDisconnectClick else onConnectClick,
                enabled = state.device != null || state.connected,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = ButtonShape,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (state.connected || state.connecting) ErrorRed else SuccessGreen,
                    contentColor = White,
                    disabledBackgroundColor = LightGrey,
                    disabledContentColor = MediumGrey
                )
            ) {
                Text(
                    when {
                        state.connecting -> "…"
                        state.connected -> stringResource(R.string.ble_disconnect)
                        else -> stringResource(R.string.ble_connect)
                    },
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun LiveExerciseCard(
    state: BleUiState,
    targetReps: Int,
    targetSets: Int,
    rollRaiseTargetDeg: Float,
    rollLowerTargetDeg: Float,
    onClearChart: () -> Unit
) {
    var technicalDetails by rememberSaveable { mutableStateOf(false) }
    val reps = (state.sensorReps ?: 0).coerceIn(0, targetReps.coerceAtLeast(0))
    val hold = if (state.sessionRunning) sensorHoldProgress(state.sensorStatus) else null
    val duration = (state.profile?.holdTimeMs ?: 0) / 1000f
    val remaining = hold?.let { kotlin.math.ceil(duration * (1f - it)).toInt() }
    val instruction = when {
        state.sessionReadyToSave || state.exerciseComplete -> stringResource(R.string.ui_set_complete)
        !state.connected -> stringResource(R.string.ui_connect_the_sensors_to_continue)
        state.sessionStarting -> stringResource(R.string.ui_starting)
        state.sessionRunning -> stringResource(sensorMovementInstruction(state.sensorStatus))
        else -> stringResource(R.string.ui_ready_to_start)
    }
    Card(Modifier.fillMaxWidth(), shape = CardShape, backgroundColor = CardBackground, elevation = 2.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.repetition_progress, reps, targetReps), style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.set_progress, state.currentSet, targetSets), style = MaterialTheme.typography.body2, color = MediumGrey)
            }
            LinearProgressIndicator((reps.toFloat() / targetReps.coerceAtLeast(1)).coerceIn(0f, 1f),
                Modifier.fillMaxWidth().height(4.dp), color = MediumBlue, backgroundColor = LightGrey)
            Text(instruction, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.SemiBold,
                color = if (state.sensorStatus.startsWith("LIMIT")) MaterialTheme.colors.error else MediumBlue)
            Box(Modifier.fillMaxWidth().height(300.dp).background(SurfaceLight, CardShape)) {
                RollLiveChart(state.rollHistory, rollRaiseTargetDeg, rollLowerTargetDeg,
                    Modifier.fillMaxSize().padding(top = 100.dp, bottom = 38.dp))
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top) {
                    Column {
                        Text(stringResource(R.string.ui_current_angle), style = MaterialTheme.typography.caption, color = DarkGrey)
                        Text(state.sensorAngle?.let { "${displaySensorNumber(it)}°" } ?: "—",
                            style = MaterialTheme.typography.h3, fontWeight = FontWeight.Bold, color = MediumBlue)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(if (remaining != null) stringResource(R.string.ui_time_remaining) else stringResource(R.string.ui_hold_at_target), style = MaterialTheme.typography.caption, color = DarkGrey)
                        Text(if (remaining != null) "≈ $remaining s" else "${displaySensorNumber(duration)} s",
                            style = MaterialTheme.typography.h4, fontWeight = FontWeight.Bold,
                            color = if (remaining != null) SuccessGreen else MediumBlue)
                        if (hold != null) LinearProgressIndicator(hold, Modifier.width(100.dp).height(4.dp), color = SuccessGreen)
                    }
                }
                Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.sensor_target_bound, displaySensorNumber(rollRaiseTargetDeg)), color = SuccessGreen, style = MaterialTheme.typography.caption)
                    Text(stringResource(R.string.sensor_return_bound, displaySensorNumber(rollLowerTargetDeg)), color = WarningOrange, style = MaterialTheme.typography.caption)
                }
            }
            TextButton(onClick = { technicalDetails = !technicalDetails }, contentPadding = PaddingValues(0.dp)) {
                Text(if (technicalDetails) stringResource(R.string.ui_hide_details) else stringResource(R.string.ui_sensor_details), style = MaterialTheme.typography.caption)
            }
            if (technicalDetails) {
                Text(stringResource(R.string.sensor_axis_readings, state.sensorPitch?.let(::displaySensorNumber) ?: "—", state.sensorRoll?.let(::displaySensorNumber) ?: "—"),
                    style = MaterialTheme.typography.body2)
                Text(stringResource(R.string.ui_0_is_the_calibrated_position_the_chart_shows_the_exercise_angle), style = MaterialTheme.typography.caption)
                TextButton(onClick = onClearChart, enabled = state.rollHistory.size >= 2) { Text(stringResource(R.string.ui_clear_chart)) }
            }
        }
    }
}

@Composable
private fun RollLiveChart(
    rollHistory: List<Float>,
    rollTargetMax: Float,
    rollTargetMin: Float,
    modifier: Modifier = Modifier
) {
    val gridColor = Color.LightGray.copy(alpha = 0.35f)
    val maxLine = SuccessGreen.copy(alpha = 0.85f)
    val minLine = WarningOrange.copy(alpha = 0.85f)

    Canvas(
        modifier = modifier.background(
            LightGrey.copy(alpha = 0.35f),
            CardShape
        )
    ) {
        val padPx = 12.dp.toPx()
        val innerW = size.width - 2 * padPx
        val innerH = size.height - 2 * padPx

        val histMin = rollHistory.minOrNull()
        val histMax = rollHistory.maxOrNull()
        val dataMin = minOf(0f, rollTargetMin, rollTargetMax, histMin ?: 0f)
        val dataMax = maxOf(rollTargetMin, rollTargetMax, histMax ?: rollTargetMax)
        val span = (dataMax - dataMin).coerceAtLeast(8f)
        val yMin = dataMin - span * 0.08f
        val yMax = dataMax + span * 0.08f
        val ySpan = (yMax - yMin).coerceAtLeast(1f)

        fun yFor(v: Float) = padPx + innerH - (v - yMin) / ySpan * innerH

        drawLine(
            color = gridColor,
            start = Offset(padPx, padPx + innerH * 0.5f),
            end = Offset(padPx + innerW, padPx + innerH * 0.5f),
            strokeWidth = 1.dp.toPx()
        )

        val yMaxLine = yFor(rollTargetMax)
        val yMinLine = yFor(rollTargetMin)
        drawLine(
            color = MediumGrey,
            start = Offset(padPx, yFor(0f)),
            end = Offset(padPx + innerW, yFor(0f)),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
        )
        drawLine(
            color = maxLine,
            start = Offset(padPx, yMaxLine),
            end = Offset(padPx + innerW, yMaxLine),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = minLine,
            start = Offset(padPx, yMinLine),
            end = Offset(padPx + innerW, yMinLine),
            strokeWidth = 2.dp.toPx()
        )

        if (rollHistory.size < 2) return@Canvas

        val strokeW = 3.dp.toPx()
        val path = Path()
        rollHistory.forEachIndexed { i, v ->
            val t = i.toFloat() / (rollHistory.size - 1).coerceAtLeast(1)
            val x = padPx + t * innerW
            val y = yFor(v)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = MediumBlue,
            style = Stroke(
                width = strokeW,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
