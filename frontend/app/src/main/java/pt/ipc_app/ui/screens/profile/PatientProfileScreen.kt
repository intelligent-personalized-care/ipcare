package pt.ipc_app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import pt.ipc_app.ui.components.ScreenHeader
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.service.models.users.PatientOutput
import pt.ipc_app.ui.components.ButtonToUpdatePicture
import pt.ipc_app.ui.components.CircularButton
import pt.ipc_app.ui.components.ProgressState
import pt.ipc_app.ui.components.TextEmail
import pt.ipc_app.ui.theme.*
import java.util.*

@Composable
fun PatientProfileScreen(
    patient: PatientOutput?,
    profilePicture: @Composable () -> Unit = { },
    updateProfilePictureState: ProgressState = ProgressState.IDLE,
    onUpdateProfilePicture: () -> Unit = { },
    onSuccessUpdateProfilePicture: () -> Unit = { },
    onLogout: () -> Unit = { }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(title = stringResource(R.string.profile_title))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            profilePicture()
            ButtonToUpdatePicture(
                updateProfilePictureState = updateProfilePictureState,
                onUpdateProfilePicture = onUpdateProfilePicture,
                onSuccessUpdateProfilePicture = onSuccessUpdateProfilePicture,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                shape = CardShape,
                backgroundColor = SurfaceLight
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    patient?.let {
                        Text(
                            text = it.name,
                            style = MaterialTheme.typography.h6,
                            fontWeight = FontWeight.SemiBold,
                            color = MediumBlue
                        )
                        TextEmail(
                            email = it.email,
                            clickable = false,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                shape = CardShape,
                backgroundColor = SurfaceLight
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileInfoRow(label = stringResource(id = R.string.birthDate), value = patient?.birthDate ?: "-")
                    ProfileInfoRow(label = stringResource(id = R.string.weight), value = patient?.weight?.toString() ?: "-")
                    ProfileInfoRow(label = stringResource(id = R.string.height), value = patient?.height?.toString() ?: "-")
                    ProfileInfoRow(label = stringResource(id = R.string.physicalCondition), value = patient?.physicalCondition ?: "-")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CircularButton(
                icon = Icons.Default.Logout,
                color = ErrorRed,
                onClick = onLogout
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.body2,
            color = MediumGrey
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            color = MediumBlue
        )
    }
}

@Preview
@Composable
fun PatientProfileScreenPreview() {
    PatientProfileScreen(
        patient = PatientOutput(UUID.randomUUID(), "Test", "test@gmail.com", 70, 184, "joelho esquerdo", "2002-02-03")
    )
}
