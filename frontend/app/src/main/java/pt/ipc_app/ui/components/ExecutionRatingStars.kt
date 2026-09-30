package pt.ipc_app.ui.components

import pt.ipc_app.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp

@Composable
fun ExecutionRatingStars(rating: Int?, enabled: Boolean = true, onRating: ((Int) -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { value ->
            val filled = rating != null && value <= rating
            val icon = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder
            if (onRating != null) {
                IconButton(onClick = { onRating(value) }, enabled = enabled,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { selected = rating == value }) {
                    Icon(icon, stringResource(R.string.rating_star_value, value), tint = if (filled) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = .35f))
                }
            } else {
                Icon(icon, null, Modifier.size(28.dp), tint = if (filled) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = .35f))
            }
        }
        if (onRating == null && rating != null) Text(" $rating/5", style = MaterialTheme.typography.subtitle2)
    }
}
