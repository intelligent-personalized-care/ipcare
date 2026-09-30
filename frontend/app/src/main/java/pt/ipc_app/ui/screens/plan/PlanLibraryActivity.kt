package pt.ipc_app.ui.screens.plan

import pt.ipc_app.R

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.service.models.plans.*
import pt.ipc_app.ui.components.ScreenHeader
import pt.ipc_app.ui.components.bottomBar.ButtonBarType
import pt.ipc_app.ui.screens.AppViewModel
import pt.ipc_app.ui.setAppContentPhysiotherapist
import pt.ipc_app.utils.executeRequest
import pt.ipc_app.utils.viewModelInit

class PlanLibraryViewModel(private val app: DependenciesContainer) : AppViewModel() {
    val plans = MutableStateFlow<List<PlanInfoOutput>>(emptyList())
    val selected = MutableStateFlow<LibraryPlan?>(null)
    val loading = MutableStateFlow(false)
    val message = MutableStateFlow<Int?>(null)
    private var job: Job? = null
    fun load(planId: Int? = null) {
        job?.cancel()
        job = viewModelScope.launch {
            loading.value = true; message.value = null
            try {
                val session = app.sessionManager
                if (planId == null) plans.value = executeRequest { app.services.plansService.getPhysiotherapistPlans(session.userUUID, session.userLoggedIn.accessToken) }.plans
                else selected.value = executeRequest { app.services.plansService.getLibraryPlan(session.userUUID, planId, session.userLoggedIn.accessToken) }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                message.value = R.string.plan_library_load_failed
            } finally { loading.value = false }
        }
    }
    fun back() { job?.cancel(); selected.value = null; load() }
}

class PlanLibraryActivity : ComponentActivity() {
    private val viewModel by viewModels<PlanLibraryViewModel> { viewModelInit { PlanLibraryViewModel(application as DependenciesContainer) } }
    companion object {
        fun navigate(context: Context) = context.startActivity(Intent(context, PlanLibraryActivity::class.java))
    }
    override fun onResume() { super.onResume(); if (viewModel.selected.value == null) viewModel.load() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.changeButtonBar(ButtonBarType.PLANS)
        setAppContentPhysiotherapist(viewModel) {
            val plans by viewModel.plans.collectAsState()
            val selected by viewModel.selected.collectAsState()
            val loading by viewModel.loading.collectAsState()
            val message by viewModel.message.collectAsState()
            BackHandler(selected != null) { viewModel.back() }
            Column(Modifier.fillMaxSize()) {
                ScreenHeader(selected?.title ?: stringResource(R.string.ui_my_plans), subtitle = if (selected == null) stringResource(R.string.ui_rehabilitation_plan_library) else stringResource(R.string.ui_plan_template))
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        if (selected != null) TextButton(onClick = viewModel::back) { Text(stringResource(R.string.ui_all_plans)) }
                        else Button(onClick = { CreatePlanActivity.navigate(this@PlanLibraryActivity) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_create_plan)) }
                    }
                    if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    if (message != null) item { Text(stringResource(message!!)); OutlinedButton(onClick = { viewModel.load(selected?.id) }) { Text(stringResource(R.string.ui_try_again)) } }
                    val detail = selected
                    if (detail == null) {
                        if (!loading && message == null && plans.isEmpty()) item { Text(stringResource(R.string.ui_you_have_no_plans_yet_create_your_first_plan_to_add_it_to_your_li)) }
                        items(plans, key = { it.id }) { plan ->
                            OutlinedButton(onClick = { viewModel.load(plan.id) }, enabled = !loading, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                                Column(Modifier.weight(1f)) { Text(plan.title, style = MaterialTheme.typography.subtitle1); Text(pluralStringResource(R.plurals.plan_duration_days, plan.days, plan.days), style = MaterialTheme.typography.caption) }
                                Text(stringResource(R.string.ui_view_plan))
                            }
                        }
                    } else {
                        detail.dailyLists.forEachIndexed { index, day ->
                            item(key = "day-$index") { Text(stringResource(R.string.plan_day_number, index + 1), style = MaterialTheme.typography.h6) }
                            if (day == null || day.exercises.isEmpty()) item { Text(stringResource(R.string.ui_rest)) }
                            else items(day.exercises, key = { "${index}-${it.id}" }) { exercise ->
                                Card(Modifier.fillMaxWidth(), elevation = 1.dp) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(exercise.title, style = MaterialTheme.typography.subtitle1)
                                        Text(stringResource(R.string.exercise_dose, exercise.sets, exercise.reps))
                                        Text(exercise.description, style = MaterialTheme.typography.body2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
