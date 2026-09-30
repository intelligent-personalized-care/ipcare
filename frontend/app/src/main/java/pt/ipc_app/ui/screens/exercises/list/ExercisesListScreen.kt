package pt.ipc_app.ui.screens.exercises.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pt.ipc_app.ui.components.ScreenHeader
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.domain.exercise.Exercise
import pt.ipc_app.domain.exercise.ExerciseInfo
import pt.ipc_app.ui.components.exercises.ExercisesInfoPagination
import pt.ipc_app.ui.theme.*

@Composable
fun ExercisesListScreen(
    exercises: List<ExerciseInfo>,
    selectedJoint: String? = null,
    onJointSelected: (String?) -> Unit = {},
    onExerciseClick: (Exercise) -> Unit = { },
    onPaginationClick: (Int) -> Unit = { },
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        ScreenHeader(
            title = stringResource(R.string.exercises_title),
            icon = Icons.Default.FitnessCenter
        )

        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(null to stringResource(R.string.label_all), "WRIST" to stringResource(R.string.label_wrist), "ELBOW" to stringResource(R.string.label_elbow), "KNEE" to stringResource(R.string.label_knee)).forEach { (joint, label) ->
                OutlinedButton(onClick = { onJointSelected(joint) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (joint == selectedJoint) MaterialTheme.colors.primary.copy(alpha = .12f) else MaterialTheme.colors.surface)) { Text(label) }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                key(selectedJoint) { ExercisesInfoPagination(
                    exercises = exercises,
                    onExerciseChosen = onExerciseClick,
                    onPaginationClick = onPaginationClick
                ) }
                if (exercises.isEmpty()) Text(stringResource(R.string.label_no_exercises_to_display), Modifier.padding(16.dp))
            }
        }
    }
}
