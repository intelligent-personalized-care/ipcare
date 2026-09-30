package pt.ipc_app.ui.components.exercises

import androidx.compose.ui.res.stringResource

import pt.ipc_app.R

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.ipc_app.service.models.exercises.PatientDailyExercises
import java.time.LocalDate
import java.util.UUID

@Composable
fun PatientExercisesToDoList(
    exercisesOfPatients: List<PatientDailyExercises>,
    onPatientSelect: (PatientDailyExercises) -> Unit = { }
) {

    if (exercisesOfPatients.isNotEmpty()) {
        LazyColumn(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .border(1.dp, Color(204, 202, 202, 255))
        ) {
            items(exercisesOfPatients) { ex ->
                PatientExercisesToDoRow(
                    patientExercise = ex,
                    onPatientSelect = onPatientSelect
                )
            }
        }
    } else {
        Text(stringResource(R.string.label_no_exercises_are_assigned_for_this_day), textAlign = TextAlign.Center)
    }

}
