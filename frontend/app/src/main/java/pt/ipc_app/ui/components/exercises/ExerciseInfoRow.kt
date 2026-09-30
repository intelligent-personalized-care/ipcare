package pt.ipc_app.ui.components.exercises

import pt.ipc_app.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pt.ipc_app.domain.exercise.Exercise
import pt.ipc_app.domain.exercise.ExerciseInfo
import pt.ipc_app.ui.components.CustomTextField
import pt.ipc_app.ui.components.TextFieldType
import pt.ipc_app.ui.theme.*

@Composable
fun ExerciseInfoRow(
    exercise: ExerciseInfo,
    clickExerciseEnabled: Boolean,
    onExerciseAdd: (Exercise) -> Unit = { }
) {
    var clicked by remember { mutableStateOf(false) }
    var sets by remember { mutableStateOf(1) }
    var reps by remember { mutableStateOf(10) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { clicked = !clicked }
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.subtitle1,
                    color = MediumBlue
                )
            }
            Row {
                if (!clickExerciseEnabled)
                    AddExerciseIcon(alreadyInDailyList = true)
                else if (clicked)
                    Icon(
                        imageVector = Icons.Default.ArrowDropUp,
                        contentDescription = stringResource(R.string.ui_collapse),
                        tint = MediumGrey
                    )
                else
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = stringResource(R.string.ui_expand),
                        tint = MediumGrey
                    )
            }
        }
        if (clicked && clickExerciseEnabled) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CustomTextField(fieldType = TextFieldType.EXERCISE_SETS, textToDisplay = sets.toString(), updateText = { sets = it.toInteger(2) }, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                    CustomTextField(fieldType = TextFieldType.EXERCISE_REPS, textToDisplay = reps.toString(), updateText = { reps = it.toInteger(3) }, keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f))
                }
                Button(onClick = {
                    onExerciseAdd(Exercise(exercise.id, exercise.title, exercise.description, sets, reps)); clicked = false
                }, enabled = sets in 1..20 && reps in 1..200, modifier = Modifier.fillMaxWidth(), shape = ButtonShape) { Text(stringResource(R.string.ui_view_exercise)) }
            }
        }
    }
}

private fun String.toInteger(maxLength: Int): Int {
    return if (isEmpty() || toIntOrNull() == null || length > maxLength) 0
    else toInt()
}

@Composable
fun AddExerciseIcon(alreadyInDailyList: Boolean) {
    Icon(
        imageVector = if (alreadyInDailyList) Icons.Default.Check else Icons.Default.Add,
        contentDescription = stringResource(R.string.label_exercise_information),
        tint = HealthMint
    )
}
