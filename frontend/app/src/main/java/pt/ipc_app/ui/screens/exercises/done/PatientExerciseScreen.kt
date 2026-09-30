package pt.ipc_app.ui.screens.exercises.done

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import pt.ipc_app.ui.components.ScreenHeader
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import pt.ipc_app.R
import pt.ipc_app.domain.exercise.Exercise
import pt.ipc_app.ui.components.CustomTextField
import pt.ipc_app.ui.components.TextFieldType
import pt.ipc_app.ui.components.VideoPlayer
import pt.ipc_app.ui.components.exercises.SensorProfileEditor
import pt.ipc_app.service.models.exercises.SensorProfile
import pt.ipc_app.service.models.exercises.SensorSession
import pt.ipc_app.ui.theme.*
import java.util.*

@Composable
fun PatientExerciseScreen(
    exercise: Exercise,
    isPatient: Boolean,
    patientExerciseUrl: String,
    setSelected: Int,
    onSetSelected: (Int) -> Unit = { },
    feedbackReceived: String? = null,
    onFeedbackSent: (String, Int?) -> Unit = { _, _ -> },
    ratingReceived: Int? = null,
    feedbackSaving: Boolean = false,
    accessToken: String? = null,
    sensorResult: SensorSession? = null,
    sensorProfile: SensorProfile? = null,
    executionMode: String? = "camera",
    withLoad: Boolean? = null,
    loadValue: Float? = null,
    loadUnit: String? = null,
    executionMessage: String = "",
    onResetProfile: () -> Unit = {},
    onSaveProfile: (SensorProfile) -> Unit = {}
) {
    var feedback by androidx.compose.runtime.saveable.rememberSaveable(exercise.exeID, setSelected, feedbackReceived) { mutableStateOf(feedbackReceived.orEmpty()) }
    var rating by androidx.compose.runtime.saveable.rememberSaveable(exercise.exeID, setSelected, ratingReceived) { mutableStateOf(ratingReceived) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(
            title = exercise.exeTitle.replaceFirstChar { it.uppercase() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!isPatient) SensorProfileEditor(profile = sensorProfile, onSave = onSaveProfile, onReset = onResetProfile)
            if (executionMessage.isNotBlank()) Text(executionMessage)
            if (executionMode != null) Text(when (withLoad) { true -> if (loadValue != null) stringResource(R.string.execution_load, String.format(java.util.Locale.getDefault(), "%g", loadValue), loadUnit.orEmpty()) else stringResource(R.string.ui_with_weights_amount_not_recorded); false -> stringResource(R.string.ui_performed_without_weights); null -> stringResource(R.string.ui_weight_use_not_specified) })
            if (executionMode == "camera") VideoPlayer(url = patientExerciseUrl, accessToken = accessToken)
            sensorResult?.let { result ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.ui_sensor_results), style = MaterialTheme.typography.h6)
                        Text(stringResource(R.string.execution_summary, result.repetitions, result.durationMs / 1000))
                        result.profile?.let { used ->
                            Text(stringResource(R.string.execution_profile, if (used.useRoll) "roll" else "pitch", used.movementDirection, used.raiseThreshold, used.lowerThreshold, used.holdTimeMs / 1000f))
                        }
                        Text(stringResource(R.string.execution_pitch_range, result.samples.minOfOrNull { it.pitch }?.let { String.format(java.util.Locale.getDefault(), "%.1f", it) } ?: "-", result.samples.maxOfOrNull { it.pitch }?.let { String.format(java.util.Locale.getDefault(), "%.1f", it) } ?: "-"))
                        Text(stringResource(R.string.execution_roll_range, result.samples.minOfOrNull { it.roll }?.let { String.format(java.util.Locale.getDefault(), "%.1f", it) } ?: "-", result.samples.maxOfOrNull { it.roll }?.let { String.format(java.util.Locale.getDefault(), "%.1f", it) } ?: "-"))
                        Text(stringResource(R.string.ui_pitch_blue_roll_green), style = MaterialTheme.typography.caption)
                        Canvas(Modifier.fillMaxWidth().height(180.dp).padding(vertical = 8.dp)) {
                            val values = result.samples.flatMap { listOf(it.pitch, it.roll) }
                            val low = (values.minOrNull() ?: 0f) - 5f
                            val high = (values.maxOrNull() ?: 0f) + 5f
                            listOf(true, false).forEach { pitch ->
                                val path = Path()
                                result.samples.forEachIndexed { index, sample ->
                                    val x = sample.elapsedMs.toFloat() / result.durationMs.coerceAtLeast(1) * size.width
                                    val angle = if (pitch) sample.pitch else sample.roll
                                    val y = size.height * (1f - (angle - low) / (high - low))
                                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path, if (pitch) MediumBlue else SuccessGreen, style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                    }
                }
            }

            // Set selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                shape = CardShape,
                backgroundColor = SurfaceLight
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.select_set),
                        style = MaterialTheme.typography.subtitle2,
                        fontWeight = FontWeight.SemiBold,
                        color = MediumBlue
                    )
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
                        val columns = ((maxWidth.value + 8f) / (48f * fontScale + 8f)).toInt().coerceIn(1, 6)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..exercise.exeSets).toList().chunked(columns).forEach { sets ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    sets.forEach { setNum ->
                                        OutlinedButton(onClick = { onSetSelected(setNum) },
                                            modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = ButtonShape,
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                backgroundColor = if (setSelected == setNum) MediumBlue else SurfaceLight,
                                                contentColor = if (setSelected == setNum) White else MediumBlue)) {
                                            Text(setNum.toString(), fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (exercise.exeSets > columns) repeat(columns - sets.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!isPatient) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 1.dp,
                    shape = CardShape,
                    backgroundColor = SurfaceLight
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.feedback_label),
                            style = MaterialTheme.typography.subtitle2,
                            fontWeight = FontWeight.SemiBold,
                            color = MediumBlue
                        )
                        Text(stringResource(R.string.ui_execution_rating), style = MaterialTheme.typography.subtitle2)
                        pt.ipc_app.ui.components.ExecutionRatingStars(rating, enabled = !feedbackSaving, onRating = { rating = it })
                        CustomTextField(
                            fieldType = TextFieldType.EXERCISE_FEEDBACK,
                            textToDisplay = feedback,
                            updateText = { feedback = it },
                            isToTrim = false,
                            iconImageVector = Icons.Default.Feedback
                        )
                        Button(
                            enabled = executionMode != null && !feedbackSaving && (feedback.isNotBlank() || rating != null),
                            onClick = {
                                onFeedbackSent(feedback, rating)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = ButtonShape,
                            colors = ButtonDefaults.buttonColors(backgroundColor = MediumBlue),
                            elevation = ButtonDefaults.elevation(defaultElevation = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(id = R.string.send_feedback), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else {
                if (feedbackReceived != null || ratingReceived != null) {
                    val text = feedbackReceived.orEmpty()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 1.dp,
                        shape = CardShape,
                        backgroundColor = SurfaceLight
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.physiotherapist_feedback),
                                style = MaterialTheme.typography.subtitle2,
                                fontWeight = FontWeight.SemiBold,
                                color = MediumBlue
                            )
                            ratingReceived?.let { pt.ipc_app.ui.components.ExecutionRatingStars(it) }
                            Text(
                                text = text,
                                style = MaterialTheme.typography.body2,
                                color = MediumGrey
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview
@Composable
fun ExerciseFeedbackScreenPreview() {
    PatientExerciseScreen(
        exercise = Exercise(UUID.randomUUID(), "Push ups", "Contract your abs and tighten your core by pulling your belly button toward your spine. \n" +
                "Inhale as you slowly bend your elbows and lower yourself to the floor, until your elbows are at a 90-degree angle.\n" +
                "Exhale while contracting your chest muscles and pushing back up through your hands, returning to the start position.", 15, 3),
        isPatient = false,
        patientExerciseUrl = "",
        setSelected = 1
    )
}
