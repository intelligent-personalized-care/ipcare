package pt.ipc_app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.ui.theme.*
import pt.ipc_app.domain.exercise.DailyExercise
import pt.ipc_app.domain.user.Role
import pt.ipc_app.service.models.exercises.PatientDailyExercises
import pt.ipc_app.preferences.UserInfo
import pt.ipc_app.service.models.requests.ConnectionRequestDecisionInput
import pt.ipc_app.service.models.requests.RequestInformation
import pt.ipc_app.service.models.users.PatientOutput
import pt.ipc_app.ui.components.*
import pt.ipc_app.ui.components.exercises.PatientExercisesToDoRow
import java.time.LocalDate
import java.util.*

const val PhysiotherapistHomeScreenTag = "PhysiotherapistHomeScreen"

@Composable
fun PhysiotherapistHomeScreen(
    physiotherapist: UserInfo,
    patientsOfPhysiotherapist: List<PatientOutput>,
    requestsOfPhysiotherapist: List<RequestInformation>,
    patientsExercisesToDo: List<PatientDailyExercises>,
    patientsExercisesToDoProgressState: ProgressState = ProgressState.IDLE,
    onDaySelected: (LocalDate) -> Unit = { },
    onPatientSelected: (PatientOutput) -> Unit = { },
    onPatientRequestDecided: (RequestInformation, ConnectionRequestDecisionInput) -> Unit = { _, _ -> },
    onPatientExercisesSelected:  (patientId: UUID, patientName: String, planDate: LocalDate) -> Unit = { _, _, _ -> }
) {
    var notificationsExpanded by remember { mutableStateOf(false) }
    var daySelected: LocalDate by remember { mutableStateOf(LocalDate.now()) }

    Column(
        modifier = Modifier
            .testTag(PhysiotherapistHomeScreenTag)
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        ScreenHeader(
            title = stringResource(id = R.string.hello) + " " + physiotherapist.name,
            compact = true,
            actions = {
                Column {
                    NotificationIcon(
                        notifications = requestsOfPhysiotherapist.isNotEmpty(),
                        onClick = { if (requestsOfPhysiotherapist.isNotEmpty()) notificationsExpanded = true }
                    )
                    if (notificationsExpanded)
                        DropdownPatientsRequestsMenu(
                            requestsOfPhysiotherapist = requestsOfPhysiotherapist,
                            onDismissRequest = { notificationsExpanded = false },
                            onPatientRequestDecided = { request, decision ->
                                onPatientRequestDecided(request, ConnectionRequestDecisionInput(decision))
                                notificationsExpanded = false
                            }
                        )
                }
            }
        )

        // Content (scrollable)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
        ) {
            // Stats Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 1.dp,
                    shape = CardShape,
                    backgroundColor = SurfaceLight
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatBox(
                            value = patientsOfPhysiotherapist.size.toString(),
                            label = stringResource(id = R.string.patients_label),
                            icon = Icons.Default.People,
                            color = MediumBlue
                        )
                        StatBox(
                            value = requestsOfPhysiotherapist.size.toString(),
                            label = stringResource(id = R.string.requests_label),
                            icon = Icons.Default.Notifications,
                            color = if (requestsOfPhysiotherapist.isNotEmpty()) WarningOrange else MediumGrey
                        )
                        StatBox(
                            value = patientsExercisesToDo.size.toString(),
                            label = stringResource(id = R.string.to_review_label),
                            icon = Icons.Default.Assignment,
                            color = HealthTeal
                        )
                    }
                }
            }

            // Patients Card
            item {
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MediumBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.my_patients),
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = MediumBlue
                            )
                        }
                        PatientsTable(
                            patients = patientsOfPhysiotherapist,
                            onPatientClick = { onPatientSelected(it) }
                        )
                    }
                }
            }

            // Day Selection
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 1.dp,
                    shape = CardShape,
                    backgroundColor = SurfaceLight
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.select_day),
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.SemiBold,
                            color = MediumBlue
                        )
                        DaysWithLocalDateRow(
                            days = daysOfWeek(LocalDate.now()),
                            daySelected = daySelected,
                            onDaySelected = {
                                daySelected = it
                                onDaySelected(daySelected)
                            }
                        )
                    }
                }
            }

            // Exercises To Review
            item {
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
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assignment,
                                contentDescription = null,
                                tint = HealthTeal,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.exercises_to_review),
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = MediumBlue
                            )
                        }

                        when {
                            patientsExercisesToDoProgressState == ProgressState.WAITING -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = MediumBlue
                                    )
                                }
                            }
                            patientsExercisesToDo.isEmpty() -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(id = R.string.no_exercises_to_review),
                                        style = MaterialTheme.typography.body2,
                                        color = MediumGrey,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            else -> {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    patientsExercisesToDo.forEach { patientEx ->
                                        PatientExercisesToDoRow(
                                            patientExercise = patientEx,
                                            onPatientSelect = {
                                                onPatientExercisesSelected(it.id, it.name, daySelected)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.h5,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = MediumGrey
        )
    }
}

@Preview
@Composable
fun PhysiotherapistHomeScreenPreview() {
    PhysiotherapistHomeScreen(
        physiotherapist = UserInfo(UUID.randomUUID().toString(), "Test", "", "", Role.PHYSIOTHERAPIST),
        patientsOfPhysiotherapist = listOf(PatientOutput(UUID.randomUUID(), "Tiago", "")),
        requestsOfPhysiotherapist = listOf(),
        patientsExercisesToDo = listOf(
            PatientDailyExercises(
                id = UUID.randomUUID(),
                name = "Tiago",
                planId = 1,
                dailyListId = 1,
                exercises = listOf(
                    DailyExercise(1, UUID.randomUUID(), "Push ups", "", "")
                )
            )
        )
    )
}