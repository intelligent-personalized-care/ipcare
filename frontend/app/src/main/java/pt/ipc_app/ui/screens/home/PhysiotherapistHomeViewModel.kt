package pt.ipc_app.ui.screens.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.ipc_app.service.UsersService
import pt.ipc_app.service.connection.APIResult
import pt.ipc_app.service.models.exercises.ExercisesOfPatients
import pt.ipc_app.service.models.requests.ConnectionRequestDecisionInput
import pt.ipc_app.service.models.requests.RequestInformation
import pt.ipc_app.service.models.requests.RequestsOfPhysiotherapist
import pt.ipc_app.service.models.sse.PostedVideo
import pt.ipc_app.service.models.sse.RequestPhysiotherapist
import pt.ipc_app.service.models.sse.SseEvent
import pt.ipc_app.service.models.users.PatientsOfPhysiotherapist
import pt.ipc_app.service.sse.EventBus
import pt.ipc_app.service.sse.SseEventListener
import pt.ipc_app.preferences.SessionManagerSharedPrefs
import pt.ipc_app.ui.components.ProgressState
import pt.ipc_app.ui.screens.AppViewModel
import java.time.LocalDate
import java.util.*

/**
 * View model for the [PhysiotherapistHomeActivity].
 *
 * @param sessionManager the manager used to handle the user session
 */
class PhysiotherapistHomeViewModel(
    private val usersService: UsersService,
    private val sessionManager: SessionManagerSharedPrefs
) : AppViewModel(), SseEventListener {

    private val _patients = MutableStateFlow<PatientsOfPhysiotherapist?>(null)
    val patients
        get() = _patients.asStateFlow()

    private val _patientsExercises = MutableStateFlow<ExercisesOfPatients?>(null)
    val patientsExercises
        get() = _patientsExercises.asStateFlow()

    private val _patientsExercisesState = MutableStateFlow(ProgressState.IDLE)
    val patientsExercisesState
        get() = _patientsExercisesState.asStateFlow()

    private val _requests = MutableStateFlow<RequestsOfPhysiotherapist?>(null)
    val requests
        get() = _requests.asStateFlow()

    fun setRequests(patientsRequests: RequestsOfPhysiotherapist) {
        _requests.value = patientsRequests
    }

    fun delRequest(patientRequest: RequestInformation) {
        _requests.value = requests.value?.copy(
            requests = requests.value?.requests?.filter { it != patientRequest } ?: listOf()
        )
    }

    fun setPatients(patientsRequests: PatientsOfPhysiotherapist) {
        _patients.value = patientsRequests
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

    fun getExercisesOfPatients(
        date: LocalDate
    ) {
        launchAndExecuteRequest(
            request = {
                _patientsExercisesState.value = ProgressState.WAITING
                usersService.getExercisesOfPatients(sessionManager.userUUID, date, sessionManager.userLoggedIn.accessToken).also {
                    if (it !is APIResult.Success) _patientsExercisesState.value = ProgressState.IDLE
                }
            },
            onSuccess = {
                _patientsExercises.value = it
                _patientsExercisesState.value = ProgressState.FINISHED
            }
        )
    }

    fun decideConnectionRequestOfPatient(
        requestId: UUID,
        requestDecision: ConnectionRequestDecisionInput
    ) {
        launchAndExecuteRequest(
            request = {
                usersService.decideConnectionRequest(
                    physiotherapistId = sessionManager.userUUID,
                    requestId = requestId,
                    requestDecision = requestDecision,
                    token = sessionManager.userLoggedIn.accessToken
                )
            },
            onSuccess = {
                _patients.value = it
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
        if (eventData is RequestPhysiotherapist && requests.value != null) {
            _requests.value = RequestsOfPhysiotherapist(
                requests.value!!.requests.plus(
                    RequestInformation(
                        requestID = eventData.requestID,
                        requestText = eventData.requestText,
                        patientID = eventData.patientID,
                        patientName = eventData.name,
                        patientEmail = ""
                    )
                )
            )
        }
        if (eventData is PostedVideo && patientsExercises.value != null) {
            _patientsExercises.value = patientsExercises.value!!.copy(
                patientsExercises = patientsExercises.value!!.patientsExercises.map { patientExercises ->
                    if (patientExercises.id == eventData.patientID)
                        patientExercises.copy(
                            exercises = patientExercises.exercises.map { exercise ->
                                if (exercise.id == eventData.exerciseID)
                                    exercise.copy(isDone = true)
                                else exercise
                            }
                        )
                    else patientExercises
                }
            )
        }
    }

}