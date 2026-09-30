package pt.ipc_app.ui.components.exercises

import androidx.compose.ui.res.stringResource

import pt.ipc_app.R

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import pt.ipc_app.domain.exercise.DailyExercise
import pt.ipc_app.ui.theme.*

@Composable
fun DailyExerciseRow(
    exercise: DailyExercise,
    onExerciseSelect: (DailyExercise) -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .clickable { onExerciseSelect(exercise) }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = exercise.title,
                style = MaterialTheme.typography.subtitle1,
                color = MediumBlue
            )
            Text(
                text = "${exercise.sets} séries · ${exercise.reps} repetições",
                style = MaterialTheme.typography.body2,
                color = MediumGrey
            )
        }
        Row {
            ExerciseIconDone(done = exercise.isDone)
        }
    }
}

@Composable
fun ExerciseIconDone(done: Boolean) {
    Icon(
        imageVector = if (done) Icons.Default.Check else Icons.Default.HourglassBottom,
        contentDescription = stringResource(R.string.label_exercise_complete),
        tint = if (done) SuccessGreen else WarningOrange,
        modifier = Modifier.size(24.dp)
    )
}
