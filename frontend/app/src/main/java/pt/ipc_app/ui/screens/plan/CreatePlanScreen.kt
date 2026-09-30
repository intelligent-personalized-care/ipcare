package pt.ipc_app.ui.screens.plan

import pt.ipc_app.R

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.gson.Gson
import pt.ipc_app.domain.exercise.ExerciseInfo
import pt.ipc_app.service.models.dailyList.DailyListInput
import pt.ipc_app.service.models.exercises.ExerciseInput
import pt.ipc_app.service.models.plans.PlanInput
import pt.ipc_app.ui.components.DaysRow
import pt.ipc_app.ui.components.ScreenHeader
import pt.ipc_app.ui.theme.*

@Composable
fun CreatePlanScreen(exercises: List<ExerciseInfo>, onPlanCreation: (PlanInput) -> Unit = {}, onExercisesPaginationClick: (Int) -> Unit = {}, saving: Boolean = false, loadingExercises: Boolean = false) {
    val gson = remember { Gson() }
    var plan by rememberSaveable(stateSaver = Saver<PlanInput, String>(save = { gson.toJson(it) }, restore = { gson.fromJson(it, PlanInput::class.java) })) {
        mutableStateOf(PlanInput("", listOf(DailyListInput())))
    }
    var known by rememberSaveable(stateSaver = Saver<List<ExerciseInfo>, String>(save = { gson.toJson(it) }, restore = { gson.fromJson(it, Array<ExerciseInfo>::class.java).toList() })) { mutableStateOf(emptyList()) }
    LaunchedEffect(exercises) { known = (exercises + known).distinctBy { it.id } }
    var day by rememberSaveable { mutableStateOf(0) }
    var catalogueOpen by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var page by rememberSaveable { mutableStateOf(0) }
    val selected = plan.dailyLists[day]?.exercises.orEmpty()
    val count = plan.dailyLists.sumOf { it?.exercises?.size ?: 0 }
    fun updateExercise(input: ExerciseInput) {
        plan = plan.copy(dailyLists = plan.dailyLists.mapIndexed { index, list ->
            if (index != day) list else {
                val entries = list?.exercises.orEmpty()
                DailyListInput(if (entries.any { it.exerciseInfoID == input.exerciseInfoID })
                    entries.map { if (it.exerciseInfoID == input.exerciseInfoID) input else it } else entries + input)
            }
        })
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        ScreenHeader(stringResource(R.string.ui_create_plan), subtitle = stringResource(R.string.ui_organise_days_and_customise_each_exercise))
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { OutlinedTextField(plan.title, { plan = plan.copy(title = it.take(50)) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.ui_plan_name)) }, placeholder = { Text(stringResource(R.string.ui_e_g_knee_mobility)) }, singleLine = true, shape = ButtonShape) }
            item {
                Text(stringResource(R.string.ui_plan_days), style = MaterialTheme.typography.h6)
                Spacer(Modifier.height(10.dp))
                DaysRow(day, plan.dailyLists.size, { day = it }, { plan = plan.addDailyList(DailyListInput()); day = plan.dailyLists.lastIndex })
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(stringResource(R.string.plan_day_number, day + 1), style = MaterialTheme.typography.h5, fontWeight = FontWeight.Bold); Text(pluralStringResource(R.plurals.exercise_count, selected.size, selected.size), color = MediumGrey) }
                    if (plan.dailyLists.size > 1) TextButton(onClick = {
                        plan = plan.copy(dailyLists = plan.dailyLists.filterIndexed { index, _ -> index != day }); day = day.coerceAtMost(plan.dailyLists.lastIndex)
                    }) { Text(stringResource(R.string.ui_remove_day)) }
                }
            }
            if (selected.isEmpty()) item {
                Card(Modifier.fillMaxWidth(), shape = CardShape, elevation = 0.dp, backgroundColor = SurfaceLight) {
                    Column(Modifier.padding(20.dp)) {
                        Icon(Icons.Default.FitnessCenter, null, tint = MediumBlue)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.ui_start_by_adding_an_exercise), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.ui_you_can_also_leave_this_as_a_rest_day), style = MaterialTheme.typography.body2, color = MediumGrey)
                    }
                }
            }
            items(selected, key = { it.exerciseInfoID }) { input ->
                val info = known.firstOrNull { it.id == input.exerciseInfoID }
                Card(Modifier.fillMaxWidth(), shape = CardShape, elevation = 0.dp) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(info?.title ?: stringResource(R.string.ui_exercise), style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.exercise_dose, input.sets, input.reps), color = MediumGrey)
                        Text(input.sensorProfile?.let { stringResource(R.string.custom_target, it.raiseThreshold) } ?: stringResource(R.string.ui_catalogue_defaults), color = MediumBlue, style = MaterialTheme.typography.caption)
                        Row {
                            TextButton(onClick = { editing = input.exerciseInfoID.toString() }, enabled = info != null) { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.ui_edit)) }
                            TextButton(onClick = { plan = plan.copy(dailyLists = plan.dailyLists.mapIndexed { index, list -> if (index == day) DailyListInput(selected.filterNot { it.exerciseInfoID == input.exerciseInfoID }) else list }) }) { Text(stringResource(R.string.ui_remove), color = ErrorRed) }
                        }
                    }
                }
            }
            item { OutlinedButton(onClick = { catalogueOpen = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = ButtonShape) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.ui_add_exercise)) } }
        }
        Surface(elevation = 3.dp) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.plan_summary, plan.dailyLists.size, count), style = MaterialTheme.typography.caption, color = MediumGrey)
                Button(onClick = { onPlanCreation(plan.copy(title = plan.title.trim())) }, enabled = !saving && plan.title.trim().length >= 3 && count > 0,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = ButtonShape) { Text(if (saving) stringResource(R.string.ui_saving) else stringResource(R.string.ui_create_plan)) }
            }
        }
    }
    if (catalogueOpen) Dialog(onDismissRequest = { catalogueOpen = false }) {
        Surface(shape = DialogShape, color = CardBackground) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(.85f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.ui_choose_exercise), Modifier.weight(1f), style = MaterialTheme.typography.h6)
                    IconButton(onClick = { catalogueOpen = false }) { Icon(Icons.Default.Close, stringResource(R.string.ui_close)) }
                }
                Text(stringResource(R.string.plan_day_settings, day + 1), style = MaterialTheme.typography.body2, color = MediumGrey)
                if (loadingExercises) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(exercises, key = { it.id }) { info ->
                        val added = selected.any { it.exerciseInfoID == info.id }
                        Surface(shape = ButtonShape, color = SurfaceLight, modifier = Modifier.fillMaxWidth().clickable { editing = info.id.toString(); catalogueOpen = false }) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) { Text(info.title, fontWeight = FontWeight.SemiBold); Text(if (added) stringResource(R.string.ui_already_added_edit) else info.type.name, style = MaterialTheme.typography.caption, color = MediumGrey) }
                                Icon(if (added) Icons.Default.Check else Icons.Default.Add, null, tint = MediumBlue)
                            }
                        }
                    }
                    if (exercises.isEmpty()) item { Text(stringResource(R.string.ui_no_exercises_on_this_page)) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { page--; onExercisesPaginationClick(page * 10) }, enabled = !loadingExercises && page > 0) { Icon(Icons.Default.ChevronLeft, stringResource(R.string.ui_previous_page)) }
                    Text(stringResource(R.string.page_number, page + 1))
                    IconButton(onClick = { page++; onExercisesPaginationClick(page * 10) }, enabled = !loadingExercises && exercises.size == 10) { Icon(Icons.Default.ChevronRight, stringResource(R.string.ui_next_page)) }
                }
            }
        }
    }
    editing?.let { id -> (exercises + known).firstOrNull { it.id.toString() == id }?.let { info ->
        PlanExerciseEditor(info, selected.firstOrNull { it.exerciseInfoID == info.id }, { editing = null }) { updateExercise(it); editing = null }
    } }
}
