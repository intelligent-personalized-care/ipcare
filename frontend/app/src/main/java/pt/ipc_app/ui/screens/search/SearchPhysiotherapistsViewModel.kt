package pt.ipc_app.ui.screens.search

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import pt.ipc_app.service.UsersService
import pt.ipc_app.service.connection.APIResult
import pt.ipc_app.service.models.users.PhysiotherapistOutput
import pt.ipc_app.preferences.SessionManagerSharedPrefs
import pt.ipc_app.ui.components.ProgressState
import pt.ipc_app.ui.screens.AppViewModel

/**
 * View model for the [SearchPhysiotherapistsActivity].
 */
class SearchPhysiotherapistsViewModel(
    private val usersService: UsersService,
    private val sessionManager: SessionManagerSharedPrefs
) : AppViewModel() {

    private val _state = MutableStateFlow(ProgressState.IDLE)
    val state
        get() = _state.asStateFlow()

    private val _physiotherapists = MutableStateFlow(listOf<PhysiotherapistOutput>())
    val physiotherapists
        get() = _physiotherapists.asStateFlow()

    /**
     * Attempts to get all physiotherapists available.
     */
    fun searchPhysiotherapists(
        name: String? = null
    ) {
        launchAndExecuteRequest(
            request = {
                _state.value = ProgressState.WAITING
                try {
                    usersService.searchPhysiotherapistsAvailable(
                        name = name?.trim()?.takeIf { it.isNotEmpty() },
                        token = sessionManager.userLoggedIn.accessToken
                    )
                } finally {
                    _state.value = ProgressState.IDLE
                }

            },
            onSuccess = {
                _state.value = ProgressState.FINISHED
                _physiotherapists.value = it.physiotherapists
            }
        )
    }

}