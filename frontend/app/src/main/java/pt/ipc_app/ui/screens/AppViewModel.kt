package pt.ipc_app.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.io.IOException
import pt.ipc_app.domain.APIException
import pt.ipc_app.service.connection.APIResult
import pt.ipc_app.service.utils.NoInternetConnection
import pt.ipc_app.service.utils.ResponseError
import pt.ipc_app.ui.components.bottomBar.ButtonBarType
import pt.ipc_app.utils.executeRequest
import java.net.UnknownHostException

/**
 * Base class for all view models that are used in the application.
 *
 * @property error the error that occurred in the view model
 */
open class AppViewModel : ViewModel() {

    private var _buttonBarClicked = MutableStateFlow(ButtonBarType.HOME)
    val buttonBarClicked
        get() = _buttonBarClicked.asStateFlow()

    private var _error = MutableStateFlow<ResponseError?>(null)
    val error
        get() = _error.asStateFlow()

    fun dismissError() { _error.value = null }

    fun changeButtonBar(buttonBarType: ButtonBarType) {
        if (buttonBarClicked.value != buttonBarType)
            _buttonBarClicked.value = buttonBarType
    }

    fun <T> launchAndExecuteRequest(
        request: suspend () -> APIResult<T>,
        onSuccess: suspend (T) -> Unit = { }
    ) {
        _error.value = null
        viewModelScope.launch {
            try {
                onSuccess(executeRequest(request))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (e is APIException) {
                    _error.value = e.error
                    Log.println(Log.WARN, "ERROR", e.error.title)
                }
                else if (e is IOException)
                    _error.value = NoInternetConnection()
                else _error.value = ResponseError(title = "Request failed", message = e.message ?: "Please try again.")
            }
        }
    }
}
