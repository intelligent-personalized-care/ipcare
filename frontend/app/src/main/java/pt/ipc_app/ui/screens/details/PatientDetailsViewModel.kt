package pt.ipc_app.ui.screens.details

import android.content.Context
import coil.request.ImageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.ipc_app.service.PlansService
import pt.ipc_app.service.UsersService
import pt.ipc_app.service.models.plans.ListOfPlans
import pt.ipc_app.service.models.users.PatientOfPhysiotherapist
import pt.ipc_app.preferences.SessionManagerSharedPrefs
import pt.ipc_app.ui.screens.AppViewModel
import java.util.*

/**
 * View model for the [PatientDetailsActivity].
 *
 * @param sessionManager the manager used to handle the user session
 */
class PatientDetailsViewModel(
    private val plansService: PlansService,
    private val usersService: UsersService,
    private val sessionManager: SessionManagerSharedPrefs
) : AppViewModel() {

    private val _patient = MutableStateFlow<PatientOfPhysiotherapist?>(null)
    val patient
        get() = _patient.asStateFlow()

    private val _plans = MutableStateFlow(ListOfPlans(listOf()))
    val plans
        get() = _plans.asStateFlow()

    fun getProfilePicture(
        context: Context,
        patientId: UUID
    ): ImageRequest =
        usersService.getProfilePicture(
            context = context,
            userId = patientId,
            token = sessionManager.userLoggedIn.accessToken
        )

    /**
     * Attempts to get the patient details.
     */
    fun getPatientDetails(
        patientId: UUID
    ) {
        launchAndExecuteRequest(
            request = {
                usersService.getPatientOfPhysiotherapist(sessionManager.userUUID, patientId, sessionManager.userLoggedIn.accessToken)
            },
            onSuccess = {
                _patient.value = it
            }
        )
    }

    /**
     * Attempts to get the physiotherapist plans.
     */
    fun getPhysiotherapistPlans() {
        launchAndExecuteRequest(
            request = {
                plansService.getPhysiotherapistPlans(
                    physiotherapistId = sessionManager.userUUID,
                    token = sessionManager.userLoggedIn.accessToken
                )
            },
            onSuccess = {
                _plans.value = it
            }
        )
    }

    /**
     * Attempts to associate a plan to a patient.
     */
    fun associatePlanToPatient(
        patientId: UUID,
        planId: Int,
        startDate: String,
        onSuccess: () -> Unit = { }
    ) {
        launchAndExecuteRequest(
            request = {
                plansService.associatePlanToPatient(
                    physiotherapistId = sessionManager.userUUID,
                    patientId = patientId,
                    token = sessionManager.userLoggedIn.accessToken,
                    planId = planId,
                    startDate = startDate
                )
            },
            onSuccess = { onSuccess() }
        )
    }

    /**
     * Attempts to disconnects a patient from his physiotherapist.
     */
    fun disconnectPhysiotherapist(
        patientId: UUID,
        onSuccess: () -> Unit = { }
    ) {
        launchAndExecuteRequest(
            request = {
                usersService.disconnectPhysiotherapist(
                    physiotherapistId = sessionManager.userUUID,
                    patientId = patientId,
                    token = sessionManager.userLoggedIn.accessToken
                )
            },
            onSuccess = { onSuccess() }
        )
    }
}