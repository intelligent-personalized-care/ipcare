package pt.ipc_app.ui.components.exercises

import pt.ipc_app.R

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

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
import pt.ipc_app.service.models.exercises.PatientDailyExercises
import pt.ipc_app.ui.theme.*

@Composable
fun PatientExercisesToDoRow(
    patientExercise: PatientDailyExercises,
    onPatientSelect: (PatientDailyExercises) -> Unit = { }
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceLight)
            .clickable { onPatientSelect(patientExercise) }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = patientExercise.name,
                style = MaterialTheme.typography.subtitle1,
                color = MediumBlue
            )
            Text(
                text = pluralStringResource(R.plurals.remaining_exercises, patientExercise.exercises.size, patientExercise.exercises.size),
                style = MaterialTheme.typography.body2,
                color = MediumGrey
            )
        }
        Row {
            Icon(
                imageVector = if (patientExercise.allExercisesDone()) Icons.Default.Check else Icons.Default.HourglassBottom,
                contentDescription = null,
                tint = if (patientExercise.allExercisesDone()) SuccessGreen else WarningOrange,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
