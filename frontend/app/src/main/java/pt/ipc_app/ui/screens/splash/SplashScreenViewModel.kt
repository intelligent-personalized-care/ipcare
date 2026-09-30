package pt.ipc_app.ui.screens.splash

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.ipc_app.domain.Plan
import pt.ipc_app.service.sse.SseService
import pt.ipc_app.service.UsersService
import pt.ipc_app.service.models.requests.RequestsOfPhysiotherapist
import pt.ipc_app.service.models.users.PatientsOfPhysiotherapist
import pt.ipc_app.service.models.users.PhysiotherapistOutput
import pt.ipc_app.preferences.SessionManagerSharedPrefs
import pt.ipc_app.ui.screens.AppViewModel

/**
 * View model for the [SplashScreenActivity].
 *
 * @param sessionManager the manager used to handle the user session
 */
class SplashScreenViewModel(
    private val usersService: UsersService,
    private val sseService: SseService,
    private val sessionManager: SessionManagerSharedPrefs
) : AppViewModel() {

    private val _physiotherapist = MutableStateFlow<PhysiotherapistOutput?>(null)
    val physiotherapist
        get() = _physiotherapist.asStateFlow()

    private val _plan = MutableStateFlow<Plan?>(null)
    val plan
        get() = _plan.asStateFlow()

    private val _patients = MutableStateFlow<PatientsOfPhysiotherapist?>(null)
    val patients
        get() = _patients.asStateFlow()

    private val _requests = MutableStateFlow<RequestsOfPhysiotherapist?>(null)
    val requests
        get() = _requests.asStateFlow()

    fun subscribe() {
        sseService.start(sessionManager.userLoggedIn.accessToken)
    }

    fun unsubscribe() {
        launchAndExecuteRequest(
            request = {
                sseService.stop(sessionManager.userLoggedIn.accessToken)
            }
        )
    }

    fun getCurrentPlanOfPatient() {
        launchAndExecuteRequest(
            request = {
                usersService.getCurrentPlanOfPatient(patientId = sessionManager.userUUID, token = sessionManager.userLoggedIn.accessToken)
            },
            onSuccess = {
                _plan.value = it
            }
        )
    }

    fun getPhysiotherapistOfPatient() {
        launchAndExecuteRequest(
            request = {
                usersService.getPhysiotherapistOfPatient(sessionManager.userUUID, sessionManager.userLoggedIn.accessToken)
            },
            onSuccess = {
                _physiotherapist.value = it
            }
        )
    }

    fun getPatientsOfPhysiotherapist() {
        launchAndExecuteRequest(
            request = {
                usersService.getPatientsOfPhysiotherapist(sessionManager.userUUID, sessionManager.userLoggedIn.accessToken)
            },
            onSuccess = {
                _patients.value = it
            }
        )
    }

    fun getRequestsOfPhysiotherapist() {
        launchAndExecuteRequest(
            request = {
                usersService.getPhysiotherapistRequests(sessionManager.userUUID, sessionManager.userLoggedIn.accessToken)
            },
            onSuccess = {
                _requests.value = it
            }
        )
    }

}