package pt.ipc_app.ui.screens.profile

import android.content.Context
import coil.request.ImageRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.ipc_app.domain.user.Role
import pt.ipc_app.service.UsersService
import pt.ipc_app.service.connection.APIResult
import pt.ipc_app.service.models.users.PatientOutput
import pt.ipc_app.service.sse.SseService
import pt.ipc_app.preferences.SessionManagerSharedPrefs
import pt.ipc_app.ui.components.ProgressState
import pt.ipc_app.ui.screens.AppViewModel
import java.io.File

/**
 * View model for the [PatientProfileActivity].
 *
 * @param sessionManager the manager used to handle the user session
 */
class PatientProfileViewModel(
    private val usersService: UsersService,
    private val sseService: SseService,
    private val sessionManager: SessionManagerSharedPrefs
) : AppViewModel() {

    private val _state = MutableStateFlow(ProgressState.IDLE)
    val state
        get() = _state.asStateFlow()

    private val _patientProfile = MutableStateFlow<PatientOutput?>(null)
    val patientProfile
        get() = _patientProfile.asStateFlow()

    fun unsubscribe() {
        launchAndExecuteRequest(
            request = {
                sseService.stop(sessionManager.userLoggedIn.accessToken)
            }
        )
    }

    fun getProfilePicture(
        context: Context
    ): ImageRequest =
        usersService.getProfilePicture(
            context = context,
            userId = sessionManager.userUUID,
            token = sessionManager.userLoggedIn.accessToken
        )

    /**
     * Attempts to get the profile of patient.
     */
    fun getProfile() {
        launchAndExecuteRequest(
            request = {
                usersService.getPatientProfile(
                    patientId = sessionManager.userUUID,
                    token = sessionManager.userLoggedIn.accessToken
                )
            },
            onSuccess = {
                _patientProfile.value = it
            }
        )
    }

    /**
     * Attempts to update the profile picture of patient.
     */
    fun updatePicture(
        image: File
    ) {
        launchAndExecuteRequest(
            request = {
                _state.value = ProgressState.WAITING
                usersService.updateProfilePicture(
                    image = image,
                    userId = sessionManager.userUUID,
                    role = Role.PATIENT,
                    token = sessionManager.userLoggedIn.accessToken
                ).also {
                    if (it !is APIResult.Success) _state.value = ProgressState.IDLE
                }
            },
            onSuccess = {
                _state.value = ProgressState.FINISHED
            }
        )
    }
}