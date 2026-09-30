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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.ui.theme.*
import pt.ipc_app.domain.DailyList
import pt.ipc_app.domain.Plan
import pt.ipc_app.domain.exercise.ExerciseTotalInfo
import pt.ipc_app.domain.user.*
import pt.ipc_app.preferences.UserInfo
import pt.ipc_app.service.models.users.PhysiotherapistOutput
import pt.ipc_app.service.models.users.Rating
import pt.ipc_app.ui.components.*
import pt.ipc_app.ui.components.exercises.DailyExerciseRow
import pt.ipc_app.ui.components.exercises.planTest
import java.time.LocalDate
import java.util.*

const val PatientHomeScreenTag = "PatientHomeScreen"

@Composable
fun PatientHomeScreen(
    patient: UserInfo,
    physiotherapist: PhysiotherapistOutput? = null,
    plan: Plan? = null,
    onPhysiotherapistClick: () -> Unit = { },
    onDayWithoutPlanSelect: (LocalDate) -> Unit = { },
    onExerciseSelect: (ExerciseTotalInfo) -> Unit = { }
) {
    var notifications by remember { mutableStateOf(false) }

    var daySelected: LocalDate by remember { mutableStateOf(LocalDate.now()) }
    var dailyListSelected: DailyList? by remember { mutableStateOf(null) }

    dailyListSelected = plan?.getListOfDayIfExists(daySelected)
    val onSelectExercise: (ExerciseTotalInfo) -> Unit = onExerciseSelect

    Column(
        modifier = Modifier
            .testTag(PatientHomeScreenTag)
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        ScreenHeader(
            title = stringResource(id = R.string.hello) + " " + patient.name,
            compact = true,
            actions = {
                NotificationIcon(
                    notifications = notifications,
                    onClick = { if (notifications) notifications = false }
                )
            }
        )

        // Content (scrollable) - single LazyColumn to avoid nesting with DailyExercisesList's LazyColumn
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp)
        ) {
            // Physiotherapist Card or "Find a physiotherapist" card
            item {
                if (physiotherapist != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onPhysiotherapistClick),
                        elevation = 1.dp,
                        shape = CardShape,
                        backgroundColor = SurfaceLight
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = stringResource(R.string.label_physiotherapist),
                                tint = MediumBlue,
                                modifier = Modifier.size(40.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.your_physiotherapist),
                                    style = MaterialTheme.typography.caption,
                                    color = MediumGrey
                                )
                                Text(
                                    text = physiotherapist.name,
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MediumBlue
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = stringResource(R.string.label_view_details),
                                tint = MediumGrey
                            )
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onPhysiotherapistClick),
                        elevation = 1.dp,
                        shape = CardShape,
                        backgroundColor = SurfaceLight
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = MediumBlue,
                                modifier = Modifier.size(40.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.find_physiotherapist_title),
                                    style = MaterialTheme.typography.h6,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MediumBlue
                                )
                                Text(
                                    text = stringResource(id = R.string.find_physiotherapist_subtitle),
                                    style = MaterialTheme.typography.body2,
                                    color = MediumGrey
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = stringResource(R.string.label_search_physiotherapists),
                                tint = MediumGrey
                            )
                        }
                    }
                }
            }

            // Plan Card
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
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = stringResource(R.string.label_plan),
                                tint = MediumBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (plan != null) plan.title else "No Plan Assigned",
                                style = MaterialTheme.typography.h6,
                                fontWeight = FontWeight.Bold,
                                color = if (plan != null) MediumBlue else MediumGrey
                            )
                        }

                        if (plan != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = HealthTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${plan.dailyLists.size} days",
                                        style = MaterialTheme.typography.body2,
                                        color = MediumGrey
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = stringResource(id = R.string.no_plan_yet),
                                style = MaterialTheme.typography.body2,
                                color = MediumGrey
                            )
                        }
                    }
                }
            }

            // Day Selection
            if (plan != null) {
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
                                    dailyListSelected = plan.getListOfDayIfExists(it)
                                    if (dailyListSelected == null) onDayWithoutPlanSelect(daySelected)
                                }
                            )
                        }
                    }
                }


                val dailyList = dailyListSelected
                if (dailyList != null) {
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
                                    text = stringResource(id = R.string.todays_exercises),
                                    style = MaterialTheme.typography.subtitle1,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MediumBlue
                                )
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    dailyList.exercises.forEach { ex ->
                                        DailyExerciseRow(
                                            exercise = ex,
                                            onExerciseSelect = { selectedEx ->
                                                onSelectExercise(
                                                    ExerciseTotalInfo(
                                                        planId = plan.id,
                                                        dailyListId = dailyList.id,
                                                        exercise = selectedEx
                                                    )
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
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
                                    .padding(20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MediumGrey,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(id = R.string.no_exercises_this_day),
                                    style = MaterialTheme.typography.body2,
                                    color = MediumGrey,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PatientHomeScreenWithoutPhysiotherapistAndPlanPreview() {
    PatientHomeScreen(
        patient = UserInfo(UUID.randomUUID().toString(), "Test", "", "", Role.PATIENT)
    )
}

@Preview
@Composable
fun PatientHomeScreenWithoutPlanPreview() {
    PatientHomeScreen(
        patient = UserInfo(UUID.randomUUID().toString(), "Test", "", "", Role.PATIENT),
        physiotherapist = PhysiotherapistOutput(UUID.randomUUID(), "Miguel", "miguel@gmail.com", Rating(4.8F, 3))
    )
}

@Preview
@Composable
fun PatientHomeScreenPreview() {
    PatientHomeScreen(
        patient = UserInfo(UUID.randomUUID().toString(), "Test", "", "", Role.PATIENT),
        physiotherapist = PhysiotherapistOutput(UUID.randomUUID(), "Miguel", "miguel@gmail.com", Rating(4.8F, 3)),
        plan = planTest
    )
}