package pt.ipc_app.ui.screens.exercises.sensor

import pt.ipc_app.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.ipc_app.ble.BleUiState
import pt.ipc_app.service.models.exercises.SensorProfile
import pt.ipc_app.ui.theme.*
import java.util.Locale

internal fun displaySensorNumber(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString() else String.format(Locale.getDefault(), "%.1f", value)

@Composable
internal fun SensorTargets(profile: SensorProfile, state: BleUiState? = null) {
    var details by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), shape = CardShape, backgroundColor = SurfaceLight) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.ui_your_movement), style = MaterialTheme.typography.h6, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(stringResource(R.string.ui_start) to "0°", stringResource(R.string.ui_target) to "≥ ${displaySensorNumber(profile.raiseThreshold)}°",
                    stringResource(R.string.ui_hold) to "${displaySensorNumber(profile.holdTimeMs / 1000f)} s").forEach { (label, value) ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(label, style = MaterialTheme.typography.body2, color = DarkGrey)
                        Text(value, style = MaterialTheme.typography.h5, color = MediumBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(stringResource(R.string.sensor_return_instruction, displaySensorNumber(profile.lowerThreshold)), style = MaterialTheme.typography.body2)
            TextButton(onClick = { details = !details }) { Text(if (details) stringResource(R.string.ui_less_information) else stringResource(R.string.ui_how_does_it_work)) }
            if (details) Text(stringResource(R.string.ui_reach_the_target_and_hold_for_the_specified_duration_then_return), style = MaterialTheme.typography.body2)
            if (state != null) {
                Divider()
                val angle = state.sensorAngle
                Text(stringResource(R.string.ui_current_angle), style = MaterialTheme.typography.body2)
                Text(angle?.let { "${displaySensorNumber(it)}°" } ?: "—",
                    style = MaterialTheme.typography.h3, color = MediumBlue, fontWeight = FontWeight.Bold)
                if (state.sessionRunning) {
                    val progress = sensorHoldProgress(state.sensorStatus)
                    Text(stringResource(sensorMovementInstruction(state.sensorStatus)), fontWeight = FontWeight.SemiBold)
                    if (progress != null) {
                        val remaining = profile.holdTimeMs / 1000f * (1f - progress)
                        Text(stringResource(R.string.sensor_remaining_time, displaySensorNumber(remaining)),
                            style = MaterialTheme.typography.h6, color = SuccessGreen)
                        LinearProgressIndicator(progress, Modifier.fillMaxWidth().height(10.dp),
                            color = SuccessGreen, backgroundColor = LightGrey)

                    }
                } else if (state.sessionReadyToSave) Text(stringResource(R.string.ui_set_complete_save_the_result_to_continue), color = SuccessGreen)
            }
        }
    }
}
