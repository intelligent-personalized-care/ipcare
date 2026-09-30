package pt.ipc_app.ui.screens.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.service.models.users.PhysiotherapistOutput
import pt.ipc_app.service.models.users.Rating
import pt.ipc_app.ui.components.ScreenHeader
import pt.ipc_app.ui.components.*
import pt.ipc_app.ui.components.TextFieldType
import java.util.*

@Composable
fun PhysiotherapistDetailsScreen(
    physiotherapist: PhysiotherapistOutput,
    profilePicture: @Composable () -> Unit = { },
    onSendEmailRequest: () -> Unit = { },
    onRequestedConnection: (String) -> Unit = { },
    onRemovePatient: () -> Unit = { },
    onRatedPhysiotherapist: (Int) -> Unit = { }
) {
    var comment by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.physiotherapist_details_title))
        Column(
            Modifier.fillMaxWidth().weight(1f)
                .verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Card(Modifier.fillMaxWidth(), shape = pt.ipc_app.ui.theme.CardShape, elevation = 1.dp) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    profilePicture()
                    Text(physiotherapist.name, style = MaterialTheme.typography.h6, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    TextEmail(email = physiotherapist.email, onClick = onSendEmailRequest)
                    PhysiotherapistRating(rating = physiotherapist.rating)
                }
            }
            if (!physiotherapist.isMyPhysiotherapist && !physiotherapist.requested) {
                Card(Modifier.fillMaxWidth(), shape = pt.ipc_app.ui.theme.CardShape, elevation = 1.dp) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(R.string.ui_request_supervision), style = MaterialTheme.typography.h6)
                        CustomTextField(fieldType = TextFieldType.REQUEST_CONNECTION, textToDisplay = comment,
                            updateText = { comment = it }, isToTrim = false, iconImageVector = Icons.Default.Comment)
                        Button(onClick = { onRequestedConnection(comment) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = pt.ipc_app.ui.theme.ButtonShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                            Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.ui_send_request), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
            } else if (physiotherapist.requested && !physiotherapist.isMyPhysiotherapist) {
                Text(stringResource(R.string.ui_supervision_request_sent), style = MaterialTheme.typography.body1)
            }
            if (physiotherapist.isMyPhysiotherapist) {
                if (!physiotherapist.hasRated) {
                    Card(Modifier.fillMaxWidth(), shape = pt.ipc_app.ui.theme.CardShape, elevation = 1.dp) {
                        RatePhysiotherapist(Modifier.padding(16.dp), onSubmitRating = onRatedPhysiotherapist)
                    }
                }
                OutlinedButton(onClick = onRemovePatient,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = pt.ipc_app.ui.theme.ButtonShape,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.error)) {
                    Icon(Icons.Default.PersonRemove, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ui_remove_connection), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }
}

@Preview
@Composable
fun PhysiotherapistDetailsScreenPreview() {
    PhysiotherapistDetailsScreen(
        physiotherapist = PhysiotherapistOutput(UUID.randomUUID(), "Mike", "", Rating(4.8F, 3))
    )
}