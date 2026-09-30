package pt.ipc_app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pt.ipc_app.ui.theme.MediumBlue
import pt.ipc_app.ui.theme.MediumGrey

@Composable
fun TextEmail(
    email: String,
    clickable: Boolean = true,
    onClick: () -> Unit = { },
    modifier: Modifier = Modifier
) {
    Row(
        modifier = if (clickable) modifier.clickable { onClick() } else modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (clickable)
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                tint = MediumBlue,
                modifier = Modifier.padding(end = 8.dp)
            )
        Text(
            text = email,
            style = MaterialTheme.typography.body2,
            color = MediumGrey
        )
    }
}