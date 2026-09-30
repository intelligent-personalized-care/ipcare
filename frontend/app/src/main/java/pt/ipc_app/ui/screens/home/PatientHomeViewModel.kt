package pt.ipc_app.ui.screens.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.ipc_app.domain.Plan
import pt.ipc_app.service.UsersService
import pt.ipc_app.service.models.sse.RequestAcceptance
import pt.ipc_app.service.models.sse.SseEvent
import pt.ipc_app.service.models.users.PhysiotherapistOutput
import pt.ipc_app.service.sse.EventBus
import pt.ipc_app.service.sse.SseEventListener
import pt.ipc_app.preferences.SessionManagerSharedPrefs
import pt.ipc_app.ui.screens.AppViewModel
import java.time.LocalDate

/**
 * View model for the [PatientHomeActivity].
 *
 * @param sessionManager the manager used to handle the user session
 */
class PatientHomeViewModel(
    private val usersService: UsersService,
    private val sessionManager: SessionManagerSharedPrefs
) : AppViewModel(), SseEventListener {

    private val _physiotherapist = MutableStateFlow<PhysiotherapistOutput?>(null)
    val physiotherapist
        get() = _physiotherapist.asStateFlow()

    private val _plan = MutableStateFlow<Plan?>(null)
    var selectedDate: LocalDate = LocalDate.now()
        private set
    val plan
        get() = _plan.asStateFlow()

    fun getCurrentPlanOfPatient(date: LocalDate = LocalDate.now()) {
        selectedDate = date
        launchAndExecuteRequest(
            request = {
                usersService.getCurrentPlanOfPatient(sessionManager.userUUID, date, sessionManager.userLoggedIn.accessToken)
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

    init {
        EventBus.registerListener(this)
    }

    override fun onCleared() {
        EventBus.unregisterListener(this)
        super.onCleared()
    }

    override fun onEventReceived(eventData: SseEvent) {
        if (eventData is RequestAcceptance)
            _physiotherapist.value = eventData.physiotherapist
    }

}
