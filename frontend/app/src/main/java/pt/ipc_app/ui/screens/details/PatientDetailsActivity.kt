package pt.ipc_app.ui.screens.details

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.service.models.users.PatientOutput
import pt.ipc_app.ui.components.ProfilePicture
import pt.ipc_app.ui.openSendEmail
import pt.ipc_app.ui.screens.home.PhysiotherapistHomeActivity
import pt.ipc_app.ui.screens.plan.PlanActivity
import pt.ipc_app.ui.setAppContentPhysiotherapist
import pt.ipc_app.utils.viewModelInit
import java.util.*

/**
 * The patient details activity.
 */
class PatientDetailsActivity : ComponentActivity() {

    private val viewModel by viewModels<PatientDetailsViewModel> {
        viewModelInit {
            val app = (application as DependenciesContainer)
            PatientDetailsViewModel(app.services.plansService, app.services.usersService, app.sessionManager)
        }
    }

    companion object {
        const val PATIENT = "PATIENT"
        fun navigate(context: Context, patient: PatientOutput) {
            with(context) {
                val intent = Intent(this, PatientDetailsActivity::class.java)
                intent.putExtra(PATIENT, patient)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.getPatientDetails(patient.id)
        viewModel.getPhysiotherapistPlans()

        setAppContentPhysiotherapist(viewModel) {
            val cl = viewModel.patient.collectAsState().value
            if (cl != null)
                PatientDetailsScreen(
                    patient = cl,
                    profilePicture = { ProfilePicture(imageRequest = viewModel.getProfilePicture(this, cl.id)) },
                    isMyPatient = true,
                    onSendEmailRequest = { openSendEmail(patient.email) },
                    plans = viewModel.plans.collectAsState().value.plans,
                    onRemovePatient = {
                        viewModel.disconnectPhysiotherapist(cl.id, onSuccess = ::finish)
                    },
                    onAssociatePlan = { pid, startDate ->
                        viewModel.associatePlanToPatient(
                            patientId = patient.id,
                            planId = pid,
                            startDate = startDate,
                            onSuccess = ::finish
                        )
                    },
                    onPlanSelected = {
                        PlanActivity.navigate(this, cl.id, cl.name, it)
                    }
                )
        }
    }

    @Suppress("deprecation")
    private val patient: PatientOutput by lazy {
        val patient = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(PATIENT, PatientOutput::class.java)
        else
            intent.getParcelableExtra(PATIENT)
        checkNotNull(patient)
    }
}