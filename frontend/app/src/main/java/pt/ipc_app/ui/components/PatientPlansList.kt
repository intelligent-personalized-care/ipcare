package pt.ipc_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.service.models.users.PlanOfPatient

/**
 * @param useColumnForNestedScroll When true, uses Column instead of LazyColumn so this list
 *        can be used inside a parent Column(Modifier.verticalScroll()). Use true when this
 *        list is inside another scrollable to avoid IllegalStateException (infinite height).
 */
@Composable
fun PatientPlansList(plans: List<PlanOfPatient>, onPlanClick: (PlanOfPatient) -> Unit = {}, useColumnForNestedScroll: Boolean = false) {
    if (plans.isEmpty()) {
        Text(stringResource(R.string.no_plans_yet), Modifier.padding(20.dp), color = pt.ipc_app.ui.theme.MediumGrey)
        return
    }
    if (useColumnForNestedScroll) Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        plans.forEach { PlanListCard(it, onPlanClick) }
    } else LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(plans) { PlanListCard(it, onPlanClick) }
    }
}

@Composable
private fun PlanListCard(plan: PlanOfPatient, onClick: (PlanOfPatient) -> Unit) {
    androidx.compose.material.Card(Modifier.fillMaxWidth().clickable { onClick(plan) }, shape = pt.ipc_app.ui.theme.CardShape, elevation = 0.dp) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(plan.title, style = MaterialTheme.typography.subtitle1, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = pt.ipc_app.ui.theme.MediumBlue)
            Text("${plan.startDate} - ${plan.endDate}", style = MaterialTheme.typography.caption, color = pt.ipc_app.ui.theme.MediumGrey)
        }
    }
}
