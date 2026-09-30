package pt.ipc_app.ui.components

import pt.ipc_app.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*
import pt.ipc_app.ui.theme.*

@Composable
fun DaysWithLocalDateRow(days: List<LocalDate>, daySelected: LocalDate, onDaySelected: (LocalDate) -> Unit, modifier: Modifier = Modifier) {
    var page by remember(days) { mutableStateOf(days.indexOf(daySelected).coerceAtLeast(0) / 7) }
    LaunchedEffect(days, daySelected) { page = days.indexOf(daySelected).coerceAtLeast(0) / 7 }
    val visibleDays = days.drop(page * 7).take(7)

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
        val columns = ((maxWidth.value + 4f) / (48f * fontScale + 4f)).toInt().coerceIn(1, 7)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (days.size > 7) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    androidx.compose.material.IconButton(onClick = { page-- }, enabled = page > 0) {
                        Icon(Icons.Default.ChevronLeft, "Dias anteriores", tint = if (page > 0) MediumBlue else MediumGrey)
                    }
                    Text(stringResource(R.string.days_range, page * 7 + 1, (page * 7 + 7).coerceAtMost(days.size), days.size), style = MaterialTheme.typography.caption)
                    androidx.compose.material.IconButton(onClick = { page++ }, enabled = (page + 1) * 7 < days.size) {
                        Icon(Icons.Default.ChevronRight, "Dias seguintes", tint = if ((page + 1) * 7 < days.size) MediumBlue else MediumGrey)
                    }
                }
            }
            visibleDays.chunked(columns).forEach { dates ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dates.forEach { date ->
                        val selected = date == daySelected
                        val today = date == LocalDate.now()
                        androidx.compose.material.Surface(
                            modifier = Modifier.weight(1f), shape = ButtonShape,
                            color = if (selected) MediumBlue else CardBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (today && !selected) MediumBlue else Color.Transparent)
                        ) {
                            Column(Modifier.fillMaxWidth().heightIn(min = 80.dp)
                                .selectable(selected = selected, role = androidx.compose.ui.semantics.Role.Tab, onClick = { onDaySelected(date) })
                                .padding(horizontal = 2.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).removeSuffix("."),
                                    style = MaterialTheme.typography.caption, maxLines = 1,
                                    color = if (selected) White else DarkGrey)
                                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.h6,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = if (selected) White else DarkGrey)
                                Text(if (today) "Hoje" else date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).removeSuffix("."),
                                    style = MaterialTheme.typography.caption, maxLines = 1,
                                    color = if (selected) White else DarkGrey)
                            }
                        }
                    }

                    if (visibleDays.size > columns) repeat(columns - dates.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
fun BoxDay(day: Int = -1, daySelected: Int = -1, onClick: (Int) -> Unit, modifier: Modifier = Modifier) {
    val selected = day >= 0 && day == daySelected
    androidx.compose.material.Surface(modifier = modifier, shape = ButtonShape,
        color = if (selected) MediumBlue else CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MediumBlue else Grey)) {
        Row(Modifier.heightIn(min = 52.dp).selectable(selected, role = androidx.compose.ui.semantics.Role.Tab, onClick = { onClick(day) })
            .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (day < 0) Icon(Icons.Default.Add, null, tint = MediumBlue, modifier = Modifier.size(18.dp))
            Text(if (day < 0) "Adicionar dia" else "Dia ${day + 1}", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = if (selected) White else MediumBlue)
        }
    }
}

fun daysOfWeek(
    centerDay: LocalDate
): List<LocalDate> =
    listOf(
        centerDay.minusDays(2),
        centerDay.minusDays(1),
        centerDay,
        centerDay.plusDays(1),
        centerDay.plusDays(2),
    )