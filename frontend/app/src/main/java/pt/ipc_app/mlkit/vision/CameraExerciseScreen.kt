package pt.ipc_app.mlkit.vision

import pt.ipc_app.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ipc_app.mlkit.posedetector.*
import java.util.Locale

fun cameraInstruction(state: CameraExerciseState): Int = when (state.measurement.phase) {
    CameraPhase.CALIBRATING -> R.string.ui_hold_the_starting_position
    CameraPhase.TRACKING_LOST -> R.string.ui_show_the_joint_from_the_side_to_the_camera
    CameraPhase.LIMIT -> if (!state.measurement.calibrated) R.string.ui_adjust_your_starting_position_and_calibrate_again else R.string.ui_return_to_the_starting_position_without_forcing
    CameraPhase.HOLD -> R.string.ui_hold_your_position
    CameraPhase.RETURN -> R.string.ui_return_to_the_starting_position
    CameraPhase.COMPLETE -> R.string.ui_set_complete
    CameraPhase.MOVE -> if (state.movement == CameraMovement.EXTENSION) R.string.ui_extend_slowly else R.string.ui_flex_slowly
    CameraPhase.READY -> R.string.ui_position_calibrated
    CameraPhase.POSITION -> R.string.ui_prepare_the_starting_position
}

@Composable
fun CameraExerciseScreen(
    state: CameraExerciseState, title: String, cameraReady: Boolean, cameraError: String?, voiceEnabled: Boolean,
    onVoice: (Boolean) -> Unit, onExit: () -> Unit, onFlip: () -> Unit, onSide: (BodySide) -> Unit,
    onCalibrate: () -> Unit, onRecord: () -> Unit, onSave: () -> Unit, onRetry: () -> Unit,
    onWithLoad: (Boolean) -> Unit = {},
    onLoadText: (String) -> Unit = {},
    onSaveLocally: () -> Unit = {},
    onRetryUploads: () -> Unit = {},
    onGallery: () -> Unit = {},
    preview: @Composable () -> Unit
) {
    val teal = MaterialTheme.colors.primary
    Column(Modifier.fillMaxSize().background(MaterialTheme.colors.background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit, enabled = !state.saving && !state.finalizing) { Icon(Icons.Outlined.ArrowBack, stringResource(R.string.ui_back)) }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.ui_camera_exercise), style = MaterialTheme.typography.caption, color = teal)
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 2)
            }
            IconButton(onClick = onFlip, enabled = !state.saving && !state.recording && !state.finalizing) { Icon(Icons.Outlined.Cameraswitch, stringResource(R.string.ui_switch_camera)) }
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFF102B49))) {
            preview()
            Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(Modifier.weight(1f), color = Color(0xE6102B49), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.ui_current_angle), color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
                        Text(state.measurement.angle?.let { "${it.toInt()}°" } ?: "—°", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        state.profile?.let { Text(stringResource(R.string.camera_target, it.raiseThreshold.toInt()), color = Color.White, fontSize = 16.sp) }
                    }
                }
                if (state.recording) Surface(Modifier.weight(1f), color = Color(0xE6102B49), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        val holding = state.measurement.phase == CameraPhase.HOLD
                        val seconds = (state.profile?.holdTimeMs ?: 0) * (if (holding) 1f - state.measurement.holdProgress else 1f) / 1000f
                        Text(if (holding) stringResource(R.string.ui_time_remaining) else stringResource(R.string.ui_hold_at_target), color = Color.White.copy(alpha = .85f), fontSize = 13.sp)
                        Text("${String.format(Locale.getDefault(), "%.1f", seconds)} s", color = if (holding) Color(0xFF72EDC0) else Color.White,
                            fontSize = 40.sp, fontWeight = FontWeight.Bold)
                        if (holding) LinearProgressIndicator(state.measurement.holdProgress, Modifier.fillMaxWidth(), color = Color(0xFF72EDC0), backgroundColor = Color.White.copy(alpha = .2f))
                    }
                }
            }
            if (state.recording) Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                color = Color(0xE6102B49), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(cameraInstruction(state)), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(R.string.camera_set_progress, state.currentSet, state.targetSets, (state.measurement.repetitions), state.targetReps), color = Color.White, fontSize = 16.sp)
                        Text("${state.elapsedSeconds / 60}:${(state.elapsedSeconds % 60).toString().padStart(2, '0')}", color = Color.White)
                    }
                    state.profile?.let { Text(stringResource(R.string.camera_return, it.lowerThreshold.toInt()), color = Color.White.copy(alpha = .85f), fontSize = 13.sp) }
                }
            }
            state.startCountdown?.let { seconds ->
                Surface(Modifier.align(Alignment.Center), color = Color(0xDD1B4571), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (seconds > 0) stringResource(R.string.ui_starting_in) else stringResource(R.string.ui_ready_to_start), color = Color.White)
                        Text(if (seconds > 0) "$seconds" else "✓", color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                        Text(if (seconds == 0) stringResource(R.string.ui_show_the_starting_position_to_the_camera) else stringResource(R.string.ui_hold_your_position), color = Color.White)
                    }
                }
            }
            if (!state.recording && state.joint == CameraJoint.WRIST) Text(stringResource(R.string.ui_wrist_2d_estimate), color = Color.White,
                modifier = Modifier.align(Alignment.BottomStart).background(Color(0xBB1B4571)).padding(8.dp), fontSize = 12.sp)
        }
        Column(Modifier.fillMaxWidth().heightIn(max = if (state.recording) 100.dp else 350.dp).verticalScroll(rememberScrollState()).padding(if (state.recording) 8.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (!state.recording) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.camera_set_progress, state.currentSet, state.targetSets, (if (state.pendingVideo != null) state.targetReps else state.measurement.repetitions), state.targetReps), fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.ui_voice), style = MaterialTheme.typography.caption)
                    Switch(checked = voiceEnabled, onCheckedChange = onVoice)
                }
            }
            if (!state.recording) state.profile?.let { profile ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalTile(stringResource(R.string.ui_start), "0°", Modifier.weight(1f))
                    GoalTile(stringResource(R.string.ui_target), "${profile.raiseThreshold.toInt()}°", Modifier.weight(1f))
                    GoalTile(stringResource(R.string.ui_hold), "${java.text.DecimalFormat("0.#").format(profile.holdTimeMs / 1000f)} s", Modifier.weight(1f))
                }

            }
            if (!state.recording && state.pendingUploads > 0) {
                Text(stringResource(R.string.camera_pending_uploads, state.pendingUploads), style = MaterialTheme.typography.body2)
                OutlinedButton(onClick = onRetryUploads, enabled = !state.saving && !state.finalizing && !state.loading) {
                    Text(stringResource(R.string.camera_retry_uploads))
                }
            }
            if (state.saving) LinearProgressIndicator(Modifier.fillMaxWidth())
            when {
                state.loading -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.ui_loading_plan_settings)) }
                state.error != null || cameraError != null -> {
                    Text(state.error ?: cameraError.orEmpty(), color = MaterialTheme.colors.error)
                    OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.ui_try_again)) }
                }
                state.complete -> { Text(stringResource(if (state.pendingUploads > 0) R.string.camera_recording_complete else R.string.ui_exercise_complete_138), color = teal, fontWeight = FontWeight.Bold); Button(onClick = onExit, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_finish)) } }
                state.pendingVideo != null -> Column {
                    pt.ipc_app.ui.components.LoadSelection(state.withLoad, !state.saving && !state.loadSelectionLocked, onWithLoad, state.loadText, onLoadText)
                    Button(onClick = onSave, enabled = !state.saving && pt.ipc_app.ui.components.validLoadSelection(state.withLoad, state.loadText), modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.saving) stringResource(R.string.ui_saving) else if (state.freePractice) stringResource(R.string.ui_finish_free_practice) else stringResource(R.string.ui_save_and_continue))
                    }
                    if (!state.freePractice) OutlinedButton(onClick = onSaveLocally, enabled = !state.saving && pt.ipc_app.ui.components.validLoadSelection(state.withLoad, state.loadText), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.camera_save_locally))
                    }
                    TextButton(onClick = onGallery, enabled = !state.saving) { Text(stringResource(R.string.camera_save_gallery)) }
                }
                state.finalizing -> { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(stringResource(R.string.ui_preparing_video)) }
                state.recording -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.ui_voice), style = MaterialTheme.typography.caption)
                    Switch(checked = voiceEnabled, onCheckedChange = onVoice)
                    OutlinedButton(onClick = onRecord, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.ui_stop_set)) }
                }
                else -> {
                    Text(when (state.joint) {
                        CameraJoint.WRIST -> stringResource(R.string.ui_support_your_forearm_horizontally_palm_down_film_from_the_side_at)
                        CameraJoint.ELBOW -> if (state.movement == CameraMovement.EXTENSION) stringResource(R.string.ui_film_from_the_side_support_your_arm_and_start_with_your_elbow_ben) else stringResource(R.string.ui_film_from_the_side_keep_your_arm_by_your_body_and_start_with_your)
                        else -> if (state.movement == CameraMovement.EXTENSION) stringResource(R.string.ui_sit_with_your_knee_bent_film_from_the_side_showing_hip_knee_and_a) else stringResource(R.string.ui_start_with_your_leg_extended_film_from_the_side_showing_hip_knee)
                    }, style = MaterialTheme.typography.body2)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BodySide.values().forEach { side ->
                            OutlinedButton(onClick = { onSide(side) }, modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (side == state.side) teal.copy(alpha = .12f) else Color.Transparent)) {
                                Text(if (side == BodySide.LEFT) stringResource(R.string.ui_left_side) else stringResource(R.string.ui_right_side))
                            }
                        }
                    }
                    Text(if (state.autoStartPending && state.measurement.calibrated && state.startCountdown == null)
                        stringResource(R.string.ui_return_to_the_starting_position_to_start_counting)
                        else stringResource(R.string.ui_after_calibration_the_set_starts_automatically_in_3_seconds), style = MaterialTheme.typography.caption)
                    if (state.measurement.phase == CameraPhase.CALIBRATING) LinearProgressIndicator(state.measurement.calibrationProgress, Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onCalibrate, enabled = cameraReady && !state.saving, modifier = Modifier.weight(1f)) { Text(if (state.measurement.calibrated) stringResource(R.string.ui_recalibrate) else stringResource(R.string.ui_calibrate)) }
                        Button(onClick = onRecord, enabled = !state.saving && (state.autoStartPending || (cameraReady && state.measurement.calibrated)), modifier = Modifier.weight(1f)) {
                            Text(if (state.autoStartPending) stringResource(R.string.ui_cancel_start) else stringResource(R.string.ui_prepare_to_start))
                        }
                    }
                }
            }
            state.galleryMessage?.let { Text(it, style = MaterialTheme.typography.caption) }
            state.message?.let { Text(it, style = MaterialTheme.typography.body2) }
        }
    }
}

@Composable
private fun GoalTile(label: String, value: String, modifier: Modifier) {
    Surface(modifier, color = MaterialTheme.colors.primary.copy(alpha = .07f), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.caption)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colors.primary)
        }
    }
}
