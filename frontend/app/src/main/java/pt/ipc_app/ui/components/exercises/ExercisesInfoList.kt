package pt.ipc_app.ui.components.exercises

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import pt.ipc_app.R
import pt.ipc_app.domain.exercise.Exercise
import pt.ipc_app.domain.exercise.ExerciseInfo
import pt.ipc_app.ui.theme.MediumGrey

@Composable
fun ExercisesInfoList(
    exercises: List<ExerciseInfo>,
    isClickExerciseEnabled: (ExerciseInfo) -> Boolean,
    onExerciseClick: (Exercise) -> Unit
) {
    if (exercises.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.no_exercises_available),
                style = MaterialTheme.typography.body2,
                color = MediumGrey
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            exercises.forEach { ex ->
                ExerciseInfoRow(
                    exercise = ex,
                    clickExerciseEnabled = isClickExerciseEnabled(ex),
                    onExerciseAdd = onExerciseClick
                )
            }
        }
    }
}
