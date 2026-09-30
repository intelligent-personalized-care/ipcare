package pt.ipc_app.ui.screens.plan

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.ui.screens.exercises.done.PatientExerciseActivity
import pt.ipc_app.ui.setAppContentPhysiotherapist
import pt.ipc_app.utils.viewModelInit
import java.util.*

/**
 * The plan activity.
 */
class PlanActivity : ComponentActivity() {

    private val viewModel by viewModels<PlanViewModel> {
        viewModelInit {
            val app = (application as DependenciesContainer)
            PlanViewModel(app.services.plansService, app.sessionManager)
        }
    }

    companion object {
        const val PATIENT_ID = "PATIENT_ID"
        const val PATIENT_NAME = "PATIENT_NAME"
        const val PLAN_DATE = "PLAN_DATE"
        fun navigate(context: Context, patientId: UUID, patientName: String, planDate: String) {
            with(context) {
                val intent = Intent(this, PlanActivity::class.java)
                intent.putExtra(PATIENT_ID, patientId.toString())
                intent.putExtra(PATIENT_NAME, patientName)
                intent.putExtra(PLAN_DATE, planDate)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.getPlanOfPatient(patientId, planDate)

        setAppContentPhysiotherapist(viewModel) {
            PlanScreen(
                plan = viewModel.plan.collectAsState().value,
                patientName = patientName,
                onExerciseSelect = {
                    PatientExerciseActivity.navigate(this, it, UUID.fromString(patientId))
                }
            )
        }
    }

    @Suppress("deprecation")
    private val patientId: String by lazy {
        val cId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getStringExtra(PATIENT_ID)
        else
            intent.getStringExtra(PATIENT_ID)
        checkNotNull(cId)
    }

    @Suppress("deprecation")
    private val patientName: String by lazy {
        val cName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getStringExtra(PATIENT_NAME)
        else
            intent.getStringExtra(PATIENT_NAME)
        checkNotNull(cName)
    }

    @Suppress("deprecation")
    private val planDate: String by lazy {
        val pDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getStringExtra(PLAN_DATE)
        else
            intent.getStringExtra(PLAN_DATE)
        checkNotNull(pDate)
    }
}
