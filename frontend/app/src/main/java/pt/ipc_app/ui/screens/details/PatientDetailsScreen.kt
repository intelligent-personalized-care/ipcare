package pt.ipc_app.ui.screens.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ipc_app.R
import pt.ipc_app.service.models.plans.PlanInfoOutput
import pt.ipc_app.service.models.users.PatientOfPhysiotherapist
import pt.ipc_app.ui.components.ScreenHeader
import pt.ipc_app.ui.components.*
import pt.ipc_app.ui.components.TextFieldType
import pt.ipc_app.ui.theme.*
import java.util.*

@Composable
fun PatientDetailsScreen(
    patient: PatientOfPhysiotherapist,
    profilePicture: @Composable () -> Unit = { },
    isMyPatient: Boolean = true,
    onSendEmailRequest: () -> Unit = { },
    plans: List<PlanInfoOutput> = listOf(),
    onRemovePatient: () -> Unit = { },
    onAssociatePlan: (Int, String) -> Unit = { _, _ -> },
    onPlanSelected: (String) -> Unit = { }
) {
    var showPlansOfPatient by remember { mutableStateOf(true) }
    var showPlansOfPhysiotherapist by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        ScreenHeader(title = stringResource(R.string.patient_details_title))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile card
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
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    profilePicture()
                    Text(
                        text = patient.name,
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.SemiBold,
                        color = MediumBlue
                    )
                    TextEmail(
                        email = patient.email,
                        onClick = onSendEmailRequest,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Button(
                        onClick = onRemovePatient,
                        colors = ButtonDefaults.buttonColors(backgroundColor = ErrorRed, contentColor = White),
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonRemove,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.remove_connection), fontSize = 14.sp)
                    }
                }
            }

            // Patient info card
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.birthDate) + ": ${patient.birthDate}",
                        style = MaterialTheme.typography.body2,
                        color = DarkGrey
                    )
                    Text(
                        text = stringResource(R.string.weight) + ": ${patient.weight}",
                        style = MaterialTheme.typography.body2,
                        color = DarkGrey
                    )
                    Text(
                        text = stringResource(R.string.height) + ": ${patient.height}",
                        style = MaterialTheme.typography.body2,
                        color = DarkGrey
                    )
                    Text(
                        text = stringResource(R.string.physicalCondition) + ": ${patient.physicalCondition}",
                        style = MaterialTheme.typography.body2,
                        color = DarkGrey
                    )
                }
            }

            if (isMyPatient) {
                // Tabs: Patient Plans | Associate Plan
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            showPlansOfPatient = true
                            showPlansOfPhysiotherapist = false
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (showPlansOfPatient) MediumBlue else LightGrey,
                            contentColor = if (showPlansOfPatient) White else DarkGrey
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.patient_plans),
                            style = MaterialTheme.typography.button,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                    Button(
                        onClick = {
                            showPlansOfPatient = false
                            showPlansOfPhysiotherapist = true
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (showPlansOfPhysiotherapist) MediumBlue else LightGrey,
                            contentColor = if (showPlansOfPhysiotherapist) White else DarkGrey
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.elevation(0.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.associate_plan),
                            style = MaterialTheme.typography.button,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }
                }

                if (showPlansOfPatient) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 1.dp,
                        shape = CardShape,
                        backgroundColor = SurfaceLight
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.patient_plans),
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.SemiBold,
                                color = MediumBlue,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            PatientPlansList(
                                    plans = patient.plans,
                                    onPlanClick = { onPlanSelected(it.startDate) },
                                    useColumnForNestedScroll = true
                                )
                        }
                    }
                }

                if (showPlansOfPhysiotherapist) {
                    AssociatePlanCard(
                        plans = plans,
                        onAssociatePlan = onAssociatePlan
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AssociatePlanCard(
    plans: List<PlanInfoOutput>,
    onAssociatePlan: (Int, String) -> Unit
) {
    var selectedPlan by remember { mutableStateOf<PlanInfoOutput?>(null) }
    var startDate by remember { mutableStateOf("") }
    var planSelectorExpanded by remember { mutableStateOf(false) }

    val dt = DatePicker(onDateSelected = { startDate = it })

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
            Text(
                text = stringResource(R.string.associate_plan),
                style = MaterialTheme.typography.subtitle1,
                fontWeight = FontWeight.SemiBold,
                color = MediumBlue
            )

            // Plan selector field
            Text(
                text = stringResource(R.string.choose_plan),
                style = MaterialTheme.typography.caption,
                color = MediumGrey,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = plans.isNotEmpty()) { planSelectorExpanded = !planSelectorExpanded },
                elevation = 0.dp,
                shape = RoundedCornerShape(12.dp),
                backgroundColor = Color.White,
                border = BorderStroke(1.dp, Grey.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedPlan?.title ?: stringResource(R.string.select_plan_placeholder),
                        style = MaterialTheme.typography.body1,
                        color = if (selectedPlan != null) DarkGrey else MediumGrey
                    )
                    Icon(
                        imageVector = if (planSelectorExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MediumGrey
                    )
                }
            }

            if (planSelectorExpanded && plans.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    elevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        plans.forEach { plan ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPlan = plan
                                        planSelectorExpanded = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = plan.title,
                                    style = MaterialTheme.typography.body1,
                                    color = DarkGrey
                                )
                                Text(
                                    text = "${plan.days}d",
                                    style = MaterialTheme.typography.caption,
                                    color = MediumBlue
                                )
                            }
                        }
                    }
                }
            }

            if (plans.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_plans_to_associate),
                    style = MaterialTheme.typography.body2,
                    color = MediumGrey,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Start date field
            Text(
                text = stringResource(R.string.create_plan_screen_label_startDate),
                style = MaterialTheme.typography.caption,
                color = MediumGrey,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            MyDatePicker(
                fieldType = TextFieldType.PLAN_START_DATE,
                value = startDate,
                onValueChange = { startDate = it },
                onClick = { dt.show() }
            )

            // Associate button
            Button(
                onClick = {
                    selectedPlan?.let { plan ->
                        if (startDate.isNotEmpty()) {
                            onAssociatePlan(plan.id, startDate)
                        }
                    }
                },
                enabled = selectedPlan != null && startDate.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = SuccessGreen,
                    disabledBackgroundColor = LightGrey,
                    contentColor = White,
                    disabledContentColor = MediumGrey
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.associate_button),
                    style = MaterialTheme.typography.button,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview
@Composable
fun PatientDetailsScreenPreview() {
    PatientDetailsScreen(patient = PatientOfPhysiotherapist(UUID.randomUUID(), "Mike", ""))
}
