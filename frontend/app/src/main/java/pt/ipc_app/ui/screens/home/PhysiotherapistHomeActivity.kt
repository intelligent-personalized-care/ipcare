package pt.ipc_app.ui.screens.home

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.service.models.requests.RequestsOfPhysiotherapist
import pt.ipc_app.service.models.users.PatientOutput
import pt.ipc_app.service.models.users.PatientsOfPhysiotherapist
import pt.ipc_app.ui.screens.details.PatientDetailsActivity
import pt.ipc_app.ui.screens.plan.PlanActivity
import pt.ipc_app.ui.setAppContentPhysiotherapist
import pt.ipc_app.utils.viewModelInit
import java.time.LocalDate
import java.util.*

/**
 * The physiotherapist home activity.
 */
class PhysiotherapistHomeActivity : ComponentActivity() {

    private val repo by lazy {
        (application as DependenciesContainer).sessionManager
    }

    private val viewModel by viewModels<PhysiotherapistHomeViewModel> {
        viewModelInit {
            val app = (application as DependenciesContainer)
            PhysiotherapistHomeViewModel(app.services.usersService, app.sessionManager)
        }
    }

    companion object {
        const val PATIENTS = "PATIENTS"
        const val REQUESTS = "REQUESTS"

        fun navigate(context: Context, patientsOfPhysiotherapist: PatientsOfPhysiotherapist? = null, requestsOfPhysiotherapist: RequestsOfPhysiotherapist? = null) {
            with(context) {
                val intent = Intent(this, PhysiotherapistHomeActivity::class.java)
                intent.putExtra(PATIENTS, patientsOfPhysiotherapist)
                intent.putExtra(REQUESTS, requestsOfPhysiotherapist)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (patients == null) viewModel.getPatientsOfPhysiotherapist()
        else viewModel.setPatients(patients!!)
        if (requests == null) viewModel.getRequestsOfPhysiotherapist()
        else viewModel.setRequests(requests!!)

        viewModel.getExercisesOfPatients(LocalDate.now())

        setAppContentPhysiotherapist(viewModel) {

            val patientsList = viewModel.patients.collectAsState().value?.patients
            val requestsList = viewModel.requests.collectAsState().value?.requests

            PhysiotherapistHomeScreen(
                physiotherapist = repo.userLoggedIn,
                patientsOfPhysiotherapist = patientsList ?: listOf(),
                requestsOfPhysiotherapist = requestsList ?: listOf(),
                patientsExercisesToDo = viewModel.patientsExercises.collectAsState().value?.patientsExercises ?: listOf(),
                patientsExercisesToDoProgressState = viewModel.patientsExercisesState.collectAsState().value,
                onDaySelected = { viewModel.getExercisesOfPatients(it) },
                onPatientSelected = { PatientDetailsActivity.navigate(this, it) },
                onPatientRequestDecided = { request, decision ->
                    viewModel.decideConnectionRequestOfPatient(request.requestID, decision)
                    viewModel.delRequest(request)
                },
                onPatientExercisesSelected = { patientId, patientName, planDate ->
                    PlanActivity.navigate(this, patientId, patientName, planDate.toString())
                }
            )
        }
    }

    @Suppress("deprecation")
    private val patients: PatientsOfPhysiotherapist? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(PATIENTS, PatientsOfPhysiotherapist::class.java)
        else
            intent.getParcelableExtra(PATIENTS)
    }

    @Suppress("deprecation")
    private val requests: RequestsOfPhysiotherapist? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(REQUESTS, RequestsOfPhysiotherapist::class.java)
        else
            intent.getParcelableExtra(REQUESTS)
    }
}