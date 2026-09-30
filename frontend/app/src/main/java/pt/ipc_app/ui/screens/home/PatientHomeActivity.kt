package pt.ipc_app.ui.screens.home

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.*
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.domain.Plan
import pt.ipc_app.service.models.users.PhysiotherapistOutput
import pt.ipc_app.ui.screens.exercises.info.ExerciseActivity
import pt.ipc_app.ui.screens.search.SearchPhysiotherapistsActivity
import pt.ipc_app.ui.screens.details.PhysiotherapistDetailsActivity
import pt.ipc_app.ui.screens.exercises.done.PatientExerciseActivity
import pt.ipc_app.ui.setAppContentPatient
import pt.ipc_app.utils.viewModelInit
import java.util.*

/**
 * The patient home activity.
 */
class PatientHomeActivity : ComponentActivity() {

    private val repo by lazy {
        (application as DependenciesContainer).sessionManager
    }

    private val viewModel by viewModels<PatientHomeViewModel> {
        viewModelInit {
            val app = (application as DependenciesContainer)
            PatientHomeViewModel(app.services.usersService, app.sessionManager)
        }
    }

    companion object {
        const val PHYSIOTHERAPIST = "PHYSIOTHERAPIST"
        const val PLAN = "PLAN"
        fun navigate(context: Context, physiotherapist: PhysiotherapistOutput? = null, plan: Plan? = null) {
            with(context) {
                val intent = Intent(this, PatientHomeActivity::class.java)
                intent.putExtra(PHYSIOTHERAPIST, physiotherapist)
                intent.putExtra(PLAN, plan)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (physiotherapist == null) viewModel.getPhysiotherapistOfPatient()
        if (plan == null) viewModel.getCurrentPlanOfPatient()

        setAppContentPatient(viewModel) {
            val mon = physiotherapist ?: viewModel.physiotherapist.collectAsState().value
            PatientHomeScreen(
                patient = repo.userLoggedIn,
                physiotherapist = mon,
                plan = viewModel.plan.collectAsState().value ?: plan,
                onPhysiotherapistClick = {
                    if (mon != null) PhysiotherapistDetailsActivity.navigate(this, mon.copy(isMyPhysiotherapist = true))
                    else SearchPhysiotherapistsActivity.navigate(this)
                },
                onDayWithoutPlanSelect = { viewModel.getCurrentPlanOfPatient(it) },
                onExerciseSelect = {
                    if (it.exercise.isDone)
                        PatientExerciseActivity.navigate(this, it, repo.userUUID)
                    else
                        ExerciseActivity.navigate(this, it)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.getPhysiotherapistOfPatient()
        viewModel.getCurrentPlanOfPatient(viewModel.selectedDate)
    }

    @Suppress("deprecation")
    private val physiotherapist: PhysiotherapistOutput? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(PHYSIOTHERAPIST, PhysiotherapistOutput::class.java)
        else
            intent.getParcelableExtra(PHYSIOTHERAPIST)
    }

    @Suppress("deprecation")
    private val plan: Plan? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(PLAN, Plan::class.java)
        else
            intent.getParcelableExtra(PLAN)
    }
}
