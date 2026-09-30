package pt.ipc_app.ui.components

import pt.ipc_app.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ipc_app.service.models.requests.RequestInformation
import java.util.*

@Composable
fun DropdownPatientsRequestsMenu(
    requestsOfPhysiotherapist: List<RequestInformation>,
    onPatientRequestDecided: (RequestInformation, decision: Boolean) -> Unit = { _,_ -> },
    onDismissRequest: () -> Unit = { }
) {
    var patientRequestIdToDisplay: UUID? by remember { mutableStateOf(null) }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest
    ) {
        requestsOfPhysiotherapist.forEach { patient ->
            DropdownMenuItem(onClick = { }) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.patient_request_message, patient.patientName),
                            fontSize = 14.sp,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable(
                                    interactionSource = MutableInteractionSource(),
                                    indication = null,
                                    onClick = { patientRequestIdToDisplay = patient.patientID }
                                )
                        )

                        Spacer(modifier = Modifier.weight(0.1f))
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = stringResource(R.string.ui_accept_patient),
                            tint = Color.Green,
                            modifier = Modifier.clickable {
                                onPatientRequestDecided(
                                    requestsOfPhysiotherapist.first { it.patientID == patient.patientID }, true
                                )
                            }
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.ui_decline_patient),
                            tint = Color.Red,
                            modifier = Modifier.clickable {
                                onPatientRequestDecided(
                                    requestsOfPhysiotherapist.first { it.patientID == patient.patientID },
                                    false
                                )
                            }
                        )
                    }
                    if (patientRequestIdToDisplay != null && patientRequestIdToDisplay == patient.patientID && patient.requestText != null)
                        Text(
                            text = patient.requestText,
                            style = MaterialTheme.typography.overline
                        )
                }
            }
        }
    }
}