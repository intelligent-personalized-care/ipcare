package pt.ipc_app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import pt.ipc_app.ui.components.ScreenHeader
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Logout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.service.models.users.DocState
import pt.ipc_app.service.models.users.PhysiotherapistProfile
import pt.ipc_app.service.models.users.Rating
import pt.ipc_app.ui.components.*
import pt.ipc_app.ui.theme.*
import java.util.*

@Composable
fun PhysiotherapistProfileScreen(
    physiotherapist: PhysiotherapistProfile?,
    profilePicture: @Composable () -> Unit = { },
    updateProfilePictureState: ProgressState = ProgressState.IDLE,
    onUpdateProfilePicture: () -> Unit = { },
    onSuccessUpdateProfilePicture: () -> Unit = { },
    submitCredentialDocumentState: ProgressState = ProgressState.IDLE,
    onSubmitCredentialDocument: () -> Unit = { },
    onSuccessSubmitCredentialDocument: () -> Unit = { },
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
            physiotherapist?.let {
                ButtonToUpdatePicture(
                    updateProfilePictureState = updateProfilePictureState,
                    onUpdateProfilePicture = onUpdateProfilePicture,
                    onSuccessUpdateProfilePicture = onSuccessUpdateProfilePicture,
                )
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = physiotherapist?.name ?: "",
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.SemiBold,
                        color = MediumBlue
                    )
                    TextEmail(
                        email = physiotherapist?.email ?: "",
                        clickable = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            physiotherapist?.let {
                PhysiotherapistRating(rating = it.rating)

                Spacer(modifier = Modifier.height(16.dp))

                if (it.docState == null) {
                    Button(
                        onClick = onSubmitCredentialDocument,
                        enabled = submitCredentialDocumentState != ProgressState.WAITING,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = ButtonShape,
                        colors = ButtonDefaults.buttonColors(backgroundColor = MediumBlue),
                        elevation = ButtonDefaults.elevation(defaultElevation = 2.dp)
                    ) {
                        Text(stringResource(R.string.ui_submit_credential))
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 1.dp,
                        shape = CardShape,
                        backgroundColor = SurfaceLight
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.ui_credential),
                                style = MaterialTheme.typography.subtitle1,
                                color = MediumBlue
                            )
                            when (physiotherapist.documentState()) {
                                DocState.VALID -> Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.ui_credential_approved),
                                    tint = SuccessGreen
                                )
                                DocState.WAITING -> Icon(
                                    imageVector = Icons.Default.HourglassBottom,
                                    contentDescription = stringResource(R.string.ui_credential_awaiting_approval),
                                    tint = WarningOrange
                                )
                                else -> Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = stringResource(R.string.ui_credential_rejected),
                                    tint = ErrorRed
                                )
                            }
                        }
                    }
                }

                if (submitCredentialDocumentState == ProgressState.WAITING) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MediumBlue)
                    }
                } else {
                    if (submitCredentialDocumentState == ProgressState.FINISHED && physiotherapist.docState == null)
                        onSuccessSubmitCredentialDocument()
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            CircularButton(
                icon = Icons.Default.Logout,
                color = ErrorRed,
                onClick = onLogout
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview
@Composable
fun PhysiotherapistProfileScreenPreview() {
    PhysiotherapistProfileScreen(
        physiotherapist = PhysiotherapistProfile(UUID.randomUUID(), "Test", "test@gmail.com", Rating(4F, 1), "Valid")
    )
}
