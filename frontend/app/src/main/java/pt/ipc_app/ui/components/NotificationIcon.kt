package pt.ipc_app.ui.components

import androidx.compose.material.Badge
import androidx.compose.material.BadgedBox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun NotificationIcon(notifications: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = { if (notifications) Badge() }) {
            Icon(
                imageVector = if (notifications) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                contentDescription = if (notifications) "Notificações por ler" else "Notificações",
                tint = Color.White
            )
        }
    }
}
