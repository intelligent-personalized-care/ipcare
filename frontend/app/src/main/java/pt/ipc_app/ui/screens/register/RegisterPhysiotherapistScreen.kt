package pt.ipc_app.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import pt.ipc_app.ui.components.ScreenHeader
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.domain.user.Physiotherapist
import pt.ipc_app.domain.user.User
import pt.ipc_app.service.utils.ProblemJson
import pt.ipc_app.ui.components.ProgressState
import pt.ipc_app.ui.components.CircularButton
import pt.ipc_app.ui.components.RegisterUser
import pt.ipc_app.ui.theme.*

const val RegisterPhysiotherapistScreenTag = "RegisterPhysiotherapistScreen"

@Composable
fun RegisterPhysiotherapistScreen(
    progressState: ProgressState = ProgressState.IDLE,
    error: ProblemJson? = null,
    onSaveRequest: (Physiotherapist) -> Unit
) {
    var userInfo: User? by remember { mutableStateOf(null) }
    val physiotherapistValidation = userInfo?.let {
        Physiotherapist.physiotherapistOrNull(it.name, it.email, it.password)
    }

    Column(
        modifier = Modifier
            .testTag(RegisterPhysiotherapistScreenTag)
            .fillMaxSize()
            .background(BackgroundWhite)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = stringResource(id = R.string.register_physiotherapist_screen_title)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                shape = CardShape,
                backgroundColor = SurfaceLight
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RegisterUser(
                        userValidation = { userInfo = it },
                        error = error
                    )
                }
            }

            CircularButton(
                icon = Icons.Default.Login,
                isEnabled = physiotherapistValidation != null,
                state = progressState,
                onClick = { if (physiotherapistValidation != null) onSaveRequest(physiotherapistValidation) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview
@Composable
fun RegisterPhysiotherapistScreenPreview() {
    RegisterPhysiotherapistScreen(onSaveRequest = {})
}
