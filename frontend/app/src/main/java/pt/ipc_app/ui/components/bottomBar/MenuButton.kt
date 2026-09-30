package pt.ipc_app.ui.components.bottomBar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ipc_app.ui.theme.*

@Composable
fun MenuButton(
    type: ButtonBarType,
    enable: Boolean,
    onClick: () -> Unit
) {
    val label = when (type) {
        ButtonBarType.HOME -> "Início"
        ButtonBarType.EXERCISES -> "Exercícios"
        ButtonBarType.PLANS -> "Planos"
        ButtonBarType.PROFILE -> "Perfil"
    }
    Column(Modifier.widthIn(min = 80.dp).clip(ButtonShape)
        .selectable(selected = enable, role = androidx.compose.ui.semantics.Role.Tab, onClick = { if (!enable) onClick() })
        .padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.background(if (enable) SurfaceLight else androidx.compose.ui.graphics.Color.Transparent, ButtonShape)
            .padding(horizontal = 18.dp, vertical = 6.dp)) {
            Icon(if (enable) type.iconEnabled else type.iconDisabled, null, tint = if (enable) MediumBlue else MediumGrey, modifier = Modifier.size(24.dp))
        }
        Text(label, style = MaterialTheme.typography.caption, color = if (enable) MediumBlue else MediumGrey,
            fontWeight = if (enable) FontWeight.Bold else FontWeight.Normal)
    }
}
