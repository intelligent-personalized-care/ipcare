package pt.ipc_app.ui.screens.exercises.info

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import pt.ipc_app.ui.components.ScreenHeader
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import pt.ipc_app.R
import pt.ipc_app.domain.exercise.Exercise
import pt.ipc_app.ui.components.VideoPlayer
import pt.ipc_app.ui.theme.*
import java.util.*

@Composable
fun ExerciseScreen(
    exercise: Exercise,
    exercisePreviewUrl: String,
    isToRecord: Boolean = true,
    onRecordClick: () -> Unit = { }
) {
    var isPlaying by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = exercise.exeTitle.replaceFirstChar { it.uppercase() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            VideoPlayer(url = exercisePreviewUrl, playing = isPlaying, muted = true)


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
                        text = stringResource(id = R.string.description),
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold,
                        color = MediumBlue
                    )
                    Text(
                        text = exercise.exeDescription,
                        style = MaterialTheme.typography.body2,
                        color = MediumGrey
                    )
                }
            }

            if (isToRecord) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        isPlaying = false
                        onRecordClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = ButtonShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = MediumBlue),
                    elevation = ButtonDefaults.elevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(id = R.string.start),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview
@Composable
fun ExerciseScreenPreview() {
    ExerciseScreen(
        exercise = Exercise(UUID.randomUUID(), "Push ups", "Contract your abs and tighten your core by pulling your belly button toward your spine. \n" +
                "Inhale as you slowly bend your elbows and lower yourself to the floor, until your elbows are at a 90-degree angle.\n" +
                "Exhale while contracting your chest muscles and pushing back up through your hands, returning to the start position.", 15, 3),
        exercisePreviewUrl = ""
    )
}
