package pt.ipc_app.ui.components.exercises

import pt.ipc_app.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.ipc_app.service.models.exercises.SensorProfile

@Composable
fun SensorProfileEditor(profile: SensorProfile?, onSave: (SensorProfile) -> Unit, onReset: (() -> Unit)? = null) {
    var advanced by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf(false) }
    val initial = profile
    val labels = listOf(stringResource(R.string.ui_target_angle), stringResource(R.string.ui_return_angle), stringResource(R.string.ui_minimum_raising_time_ms),
        stringResource(R.string.ui_hold_duration_seconds), stringResource(R.string.ui_repetition_cooldown_ms), stringResource(R.string.ui_minimum_speed_s),
        stringResource(R.string.ui_minimum_pitch), stringResource(R.string.ui_maximum_pitch), stringResource(R.string.ui_minimum_roll), stringResource(R.string.ui_maximum_roll))
    var values by remember(profile) { mutableStateOf(listOf(initial?.raiseThreshold, initial?.lowerThreshold,
        initial?.minRaiseTimeMs, initial?.holdTimeMs?.div(1000f), initial?.cooldownMs, initial?.minMovementSpeed,
        initial?.minPitch, initial?.maxPitch, initial?.minRoll, initial?.maxRoll).map { it?.toString() ?: "" }) }
    var useRoll by remember(profile) { mutableStateOf(initial?.useRoll ?: true) }
    var direction by remember(profile) { mutableStateOf(initial?.movementDirection ?: 1) }
    Column(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }) { Text(stringResource(R.string.ui_exercise_angles)) }
        if (expanded) {
            if (profile == null) Text(stringResource(R.string.ui_no_profile_defined_enter_suitable_parameters_for_this_exercise_an))
            Text(stringResource(R.string.ui_targets_and_timings_apply_to_camera_and_sensors_for_this_exercise))
            TextButton(onClick = { advanced = !advanced }) { Text(if (advanced) stringResource(R.string.ui_hide_advanced_options) else stringResource(R.string.ui_advanced_options)) }
            if (advanced) Text(stringResource(R.string.ui_primary_axis_limits_apply_after_reversing_direction_the_other_axi), style = MaterialTheme.typography.caption)
            if (advanced) Row { Text(stringResource(R.string.ui_use_roll_as_the_primary_axis)); Switch(checked = useRoll, onCheckedChange = { useRoll = it }) }
            if (advanced) Row { Text(stringResource(R.string.ui_reverse_sensor_direction)); Switch(checked = direction == -1, onCheckedChange = { direction = if (it) -1 else 1 }) }
            labels.forEachIndexed { index, label ->
                if (advanced || index in listOf(0, 1, 3)) OutlinedTextField(value = values[index], onValueChange = { input ->
                    values = values.toMutableList().also { it[index] = input }; validationError = false
                }, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp))
            }
            if (validationError) Text(stringResource(R.string.ui_check_angles_limits_and_timings_hold_duration_must_be_0_60_second), color = MaterialTheme.colors.error)
            Button(onClick = {
                val floats = values.map { it.replace(',', '.').toFloatOrNull() }
                val times = listOf(values[2].toIntOrNull(), floats[3]?.takeIf { it.isFinite() && it in 0f..60f }?.let { (it * 1000).toInt() }, values[4].toIntOrNull())
                if (floats.any { it == null || !it.isFinite() } || times.any { it == null }) validationError = true
                else { val candidate = SensorProfile(floats[0]!!, floats[1]!!, times[0]!!, times[1]!!, times[2]!!,
                    floats[5]!!, floats[6]!!, floats[7]!!, floats[8]!!, floats[9]!!, useRoll, direction)
                    if (candidate.isValid()) onSave(candidate) else validationError = true
                }
            }) { Text(stringResource(R.string.ui_save_custom_settings)) }
            onReset?.let { reset -> TextButton(onClick = reset) { Text(stringResource(R.string.ui_restore_catalogue_defaults)) } }
        }
    }
}
