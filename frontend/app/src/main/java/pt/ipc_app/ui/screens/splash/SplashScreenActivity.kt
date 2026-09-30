package pt.ipc_app.ui.screens.splash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import kotlinx.coroutines.*
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.R
import pt.ipc_app.domain.exercise.Exercise
import pt.ipc_app.domain.user.Role
import pt.ipc_app.domain.user.isPatient
import pt.ipc_app.ui.screens.exercises.info.ExerciseActivity
import pt.ipc_app.ui.screens.home.PatientHomeActivity
import pt.ipc_app.ui.screens.home.PhysiotherapistHomeActivity
import pt.ipc_app.ui.screens.role.ChooseRoleActivity
import pt.ipc_app.utils.viewModelInit
import java.util.UUID

/**
 * The start screen.
 */
class SplashScreenActivity: ComponentActivity() {

    private val repo by lazy {
        (application as DependenciesContainer).sessionManager
    }

    private val viewModel by viewModels<SplashScreenViewModel> {
        viewModelInit {
            val app = (application as DependenciesContainer)
            SplashScreenViewModel(app.services.usersService, app.services.sseService, app.sessionManager)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        CoroutineScope(Dispatchers.Main).launch {
            if (repo.isLoggedIn()) {
                if (repo.userLoggedIn.role.isPatient()) {
                    viewModel.getPhysiotherapistOfPatient()
                    viewModel.getCurrentPlanOfPatient()
                } else {
                    viewModel.getPatientsOfPhysiotherapist()
                    viewModel.getRequestsOfPhysiotherapist()
                }
            }

            delay(SPLASH_TIME)

            if (!repo.isLoggedIn()) {
                ChooseRoleActivity.navigate(this@SplashScreenActivity)
            } else {
                if (repo.userLoggedIn.role.isPatient()) {
                    PatientHomeActivity.navigate(this@SplashScreenActivity, viewModel.physiotherapist.value, viewModel.plan.value)
                } else {
                    PhysiotherapistHomeActivity.navigate(this@SplashScreenActivity, viewModel.patients.value, viewModel.requests.value)
                }
            }
            finish()
        }
    }

    companion object {
        const val SPLASH_TIME = 3000L
    }
}