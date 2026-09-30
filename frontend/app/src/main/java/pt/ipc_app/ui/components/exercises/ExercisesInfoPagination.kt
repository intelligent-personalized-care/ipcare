package pt.ipc_app.ui.components.exercises

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import pt.ipc_app.R
import pt.ipc_app.domain.exercise.Exercise
import pt.ipc_app.domain.exercise.ExerciseInfo
import pt.ipc_app.ui.theme.*

@Composable
fun ExercisesInfoPagination(
    exercises: List<ExerciseInfo>,
    onExerciseChosen: (Exercise) -> Unit = { },
    isClickExerciseEnabled: (ExerciseInfo) -> Boolean = { true },
    onPaginationClick: (Int) -> Unit = { },
    modifier: Modifier = Modifier
) {
    var curSkip by remember { mutableStateOf(0) }

    Column(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 1.dp,
            shape = CardShape,
            backgroundColor = SurfaceLight
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExercisesInfoList(
                    exercises = exercises,
                    isClickExerciseEnabled = isClickExerciseEnabled,
                    onExerciseClick = onExerciseChosen
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    curSkip = (curSkip - 10).coerceAtLeast(0)
                    onPaginationClick(curSkip)
                },
                enabled = curSkip > 0,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(R.string.label_previous_page),
                    tint = if (curSkip > 0) MediumBlue else MediumGrey
                )
            }
            Text(
                text = stringResource(id = R.string.page_label, (curSkip / 10) + 1),
                style = MaterialTheme.typography.body2,
                color = MediumGrey
            )
            IconButton(
                onClick = {
                    curSkip += 10
                    onPaginationClick(curSkip)
                },
                enabled = exercises.size == 10,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(R.string.label_next_page),
                    tint = if (exercises.size == 10) MediumBlue else MediumGrey
                )
            }
        }
    }
}
