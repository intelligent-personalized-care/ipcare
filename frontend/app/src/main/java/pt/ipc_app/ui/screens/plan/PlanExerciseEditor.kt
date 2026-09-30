package pt.ipc_app.ui.screens.plan

import pt.ipc_app.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pt.ipc_app.domain.exercise.ExerciseInfo
import pt.ipc_app.service.models.exercises.ExerciseInput
import pt.ipc_app.service.models.exercises.SensorProfile
import pt.ipc_app.ui.theme.*

private class PlanInputException(val resource: Int) : IllegalArgumentException()

fun planExerciseProfile(base: SensorProfile?, custom: Boolean, target: String, returned: String, hold: String): SensorProfile? {
    if (!custom) return null // Inherit the catalogue, do not freeze its current values.
    if (base == null) throw PlanInputException(R.string.plan_profile_missing)
    fun number(value: String) = value.replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
        ?: throw PlanInputException(R.string.plan_numbers_required)
    val seconds = number(hold)
    if (seconds !in 0f..60f) throw PlanInputException(R.string.plan_hold_invalid)
    return base.copy(raiseThreshold = number(target), lowerThreshold = number(returned), holdTimeMs = (seconds * 1000).toInt())
        .also { if (!it.isValid()) throw PlanInputException(R.string.plan_angles_invalid) }
}

@Composable
internal fun PlanExerciseEditor(info: ExerciseInfo, initial: ExerciseInput?, onDismiss: () -> Unit, onSave: (ExerciseInput) -> Unit) {
    val base = remember(info, initial) { initial?.sensorProfile ?: runCatching { info.movementProfile() }.getOrNull() }
    var sets by rememberSaveable(info.id) { mutableStateOf((initial?.sets ?: 2).toString()) }
    var reps by rememberSaveable(info.id) { mutableStateOf((initial?.reps ?: 10).toString()) }
    var custom by rememberSaveable(info.id) { mutableStateOf(initial?.sensorProfile != null) }
    var target by rememberSaveable(info.id) { mutableStateOf(base?.raiseThreshold?.toString() ?: "") }
    var returned by rememberSaveable(info.id) { mutableStateOf(base?.lowerThreshold?.toString() ?: "") }
    var hold by rememberSaveable(info.id) { mutableStateOf(base?.let { (it.holdTimeMs / 1000f).toString() } ?: "") }
    var error by remember { mutableStateOf<Int?>(null) }
    AlertDialog(onDismissRequest = onDismiss, shape = DialogShape,
        title = { Text(info.title, style = MaterialTheme.typography.h6) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.ui_set_the_exercise_dose_for_this_day))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DoseField(stringResource(R.string.ui_sets), sets, { sets = it }, Modifier.weight(1f))
                    DoseField(stringResource(R.string.ui_repetitions), reps, { reps = it }, Modifier.weight(1f))
                }
                if (base != null) {
                    Divider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.ui_customise_angles), style = MaterialTheme.typography.subtitle1)
                            Text(if (custom) stringResource(R.string.ui_only_for_this_exercise_in_the_plan) else stringResource(R.string.ui_use_catalogue_defaults), style = MaterialTheme.typography.caption)
                        }
                        Switch(custom, onCheckedChange = { custom = it; error = null })
                    }
                    if (custom) {
                        DoseField(stringResource(R.string.ui_target_angle), target, { target = it })
                        DoseField(stringResource(R.string.ui_return_angle), returned, { returned = it })
                        DoseField(stringResource(R.string.ui_hold_duration_seconds), hold, { hold = it })
                        Text(stringResource(R.string.plan_allowed_range, if (base.useRoll) base.minRoll else base.minPitch, if (base.useRoll) base.maxRoll else base.maxPitch), style = MaterialTheme.typography.caption)
                    } else {
                        val defaults = runCatching { info.movementProfile() }.getOrNull()
                        defaults?.let { Text(stringResource(R.string.plan_profile_summary, it.raiseThreshold, it.lowerThreshold, it.holdTimeMs / 1000f), color = MediumBlue) }
                        Text(stringResource(R.string.ui_the_app_uses_the_values_from_the_database), style = MaterialTheme.typography.caption)
                    }
                }
                error?.let { Text(stringResource(it), color = MaterialTheme.colors.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                try {
                    val s = sets.toIntOrNull(); val r = reps.toIntOrNull()
                    if (s == null || s !in 1..20 || r == null || r !in 1..200) throw PlanInputException(R.string.plan_dose_invalid)
                    onSave(ExerciseInput(info.id, s, r, planExerciseProfile(base, custom, target, returned, hold)))
                } catch (e: IllegalArgumentException) { error = (e as? PlanInputException)?.resource ?: R.string.plan_numbers_required }
            }) { Text(if (initial == null) stringResource(R.string.ui_add_to_day) else stringResource(R.string.ui_save_changes)) }
        }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ui_cancel)) } })
}

@Composable
private fun DoseField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true,
        shape = ButtonShape, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
}
