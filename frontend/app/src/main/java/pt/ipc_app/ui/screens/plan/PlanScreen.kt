package pt.ipc_app.ui.screens.plan

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import pt.ipc_app.ui.theme.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import pt.ipc_app.ui.components.ScreenHeader
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.domain.DailyList
import pt.ipc_app.domain.Plan
import pt.ipc_app.domain.exercise.ExerciseTotalInfo
import pt.ipc_app.domain.toLocalDate
import pt.ipc_app.ui.components.DaysWithLocalDateRow
import pt.ipc_app.ui.components.exercises.DailyExercisesList
import pt.ipc_app.ui.components.exercises.planTest
import java.time.LocalDate

@Composable
fun PlanScreen(
    plan: Plan?,
    patientName: String,
    onExerciseSelect: (ExerciseTotalInfo) -> Unit = { }
) {
    val days = remember(plan) { plan?.days().orEmpty() }
    var daySelected by remember(plan?.id) { mutableStateOf(LocalDate.now().takeIf { it in days } ?: days.firstOrNull() ?: LocalDate.now()) }
    val selected = plan?.getListOfDayIfExists(daySelected)
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(plan?.title ?: stringResource(R.string.plan_screen_title), subtitle = patientName.takeIf { it.isNotBlank() }?.let { "Plano de $it" })
        if (plan == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth().padding(20.dp))
        } else {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val formatter = java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                if (days.isNotEmpty()) Text("${days.first().format(formatter)} - ${days.last().format(formatter)}", style = MaterialTheme.typography.caption, color = MediumGrey)
                DaysWithLocalDateRow(days, daySelected, { daySelected = it })
                Text(pluralStringResource(R.plurals.exercise_count, selected?.exercises?.size ?: 0, selected?.exercises?.size ?: 0), style = MaterialTheme.typography.h6)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                DailyExercisesList(selected) { exercise ->
                    selected?.let { onExerciseSelect(ExerciseTotalInfo(plan.id, it.id, exercise)) }
                }
            }
        }
    }
}

@Preview
@Composable
fun PlanScreenPreview() {
    PlanScreen(plan = planTest, patientName = "Test")
}