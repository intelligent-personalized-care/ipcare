package pt.ipc_app.ui.components

import androidx.compose.ui.res.stringResource

import pt.ipc_app.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import pt.ipc_app.ui.theme.ButtonShape
import pt.ipc_app.ui.theme.MediumBlue

@Composable
fun RatePhysiotherapist(modifier: Modifier = Modifier, onSubmitRating: (Int) -> Unit = {}) {
    var stars by rememberSaveable { mutableStateOf(0) }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.label_rate_physiotherapist), style = MaterialTheme.typography.h6)
        Text(if (stars == 0) stringResource(R.string.label_select_a_rating) else stringResource(R.string.rating_star_value, stars),
            style = MaterialTheme.typography.body2)
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            (1..5).forEach { rating ->
                Surface(Modifier.weight(1f), shape = ButtonShape,
                    color = if (rating <= stars) MediumBlue.copy(alpha = .10f) else MaterialTheme.colors.surface) {
                    Box(Modifier.heightIn(min = 48.dp)
                        .selectable(selected = rating == stars, role = Role.RadioButton, onClick = { stars = rating })
                        .padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Icon(if (rating <= stars) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = stringResource(R.string.rating_star_value, rating), tint = MediumBlue, modifier = Modifier.size(28.dp))
                    }
                }
            }
        }
        Button(onClick = { onSubmitRating(stars) }, enabled = stars > 0,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = ButtonShape,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
            Text(stringResource(R.string.label_submit_rating))
        }
    }
}
