package pt.ipc_app.ui.screens.details

import android.content.Context
import coil.request.ImageRequest
import pt.ipc_app.service.UsersService
import pt.ipc_app.preferences.SessionManagerSharedPrefs
import pt.ipc_app.ui.screens.AppViewModel
import java.util.*

/**
 * View model for the [PhysiotherapistDetailsActivity].
 *
 * @param sessionManager the manager used to handle the user session
 */
class PhysiotherapistDetailsViewModel(
    private val usersService: UsersService,
    private val sessionManager: SessionManagerSharedPrefs
) : AppViewModel() {
    val currentPhysiotherapist = kotlinx.coroutines.flow.MutableStateFlow<pt.ipc_app.service.models.users.PhysiotherapistOutput?>(null)

    fun refreshPhysiotherapist() {
        launchAndExecuteRequest(
            request = { usersService.getPhysiotherapistOfPatient(sessionManager.userUUID, sessionManager.userLoggedIn.accessToken) },
            onSuccess = { currentPhysiotherapist.value = it.copy(isMyPhysiotherapist = true) }
        )
    }


    fun getProfilePicture(
        context: Context,
        physiotherapistId: UUID
    ): ImageRequest =
        usersService.getProfilePicture(
            context = context,
            userId = physiotherapistId,
            token = sessionManager.userLoggedIn.accessToken
        )

    /**
     * Attempts to connect the physiotherapist with a patient.
     */
    fun connectWithPhysiotherapist(
        physiotherapistId: UUID,
        comment: String,
        onSuccess: () -> Unit = { }
    ) {
        launchAndExecuteRequest(
            request = {
                usersService.connectPhysiotherapist(
                    physiotherapistId = physiotherapistId,
                    patientId = sessionManager.userUUID,
                    comment = comment.ifEmpty { null },
                    token = sessionManager.userLoggedIn.accessToken
                )
            },
            onSuccess = { onSuccess() }
        )
    }

    /**
     * Attempts to disconnects a patient from his physiotherapist.
     */
    fun disconnectPhysiotherapist(
        onSuccess: () -> Unit = { }
    ) {
        launchAndExecuteRequest(
            request = {
                usersService.disconnectPhysiotherapist(
                    patientId = sessionManager.userUUID,
                    token = sessionManager.userLoggedIn.accessToken
                )
            },
            onSuccess = { onSuccess() }
        )
    }

    /**
     * Attempts to rate a physiotherapist.
     */
    fun ratePhysiotherapist(
        physiotherapistId: UUID,
        stars: Int,
        onSuccess: () -> Unit = { }
    ) {
        launchAndExecuteRequest(
            request = {
                usersService.ratePhysiotherapist(
                    physiotherapistId = physiotherapistId,
                    patientId = sessionManager.userUUID,
                    stars = stars,
                    token = sessionManager.userLoggedIn.accessToken
                )
            },
            onSuccess = { onSuccess() }
        )
    }
}