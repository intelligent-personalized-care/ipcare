package pt.ipc_app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.service.models.users.PatientOutput

@Composable
fun PatientsTable(
    patients: List<PatientOutput>,
    modifier: Modifier = Modifier,
    onPatientClick: (PatientOutput) -> Unit = { }
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (patients.isEmpty()) {
            Text(
                text = stringResource(R.string.no_patients_yet),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.65f),
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            patients.forEachIndexed { index, patient ->
                key(patient.id) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button, onClick = { onPatientClick(patient) })
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = patient.name,
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (index < patients.lastIndex) {
                        Divider(color = MaterialTheme.colors.onSurface.copy(alpha = 0.08f))
                    }
                }
            }
        }
    }
}
