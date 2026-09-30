package pt.ipc_app.ui.components

import androidx.compose.ui.res.stringResource

import pt.ipc_app.R

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.ui.theme.*

@Composable
fun CircularButton(
    icon: ImageVector,
    color: Color? = null,
    isEnabled: Boolean = true,
    state: ProgressState = ProgressState.IDLE,
    onClick: () -> Unit = { }
) {
    val size = 64.dp
    val buttonColor = color ?: MediumBlue
    val scale by animateFloatAsState(
        targetValue = if (isEnabled && state != ProgressState.WAITING) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.6f)
    )

    Box(
        modifier = Modifier
            .size(size)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        if (state == ProgressState.WAITING) {
            CircularProgressIndicator(
                modifier = Modifier.size(size),
                color = buttonColor,
                strokeWidth = 4.dp
            )
        } else {
            Button(
                onClick = onClick,
                enabled = isEnabled,
                shape = ButtonShape,
                modifier = Modifier.size(size),
                colors = ButtonDefaults.buttonColors(backgroundColor = buttonColor, contentColor = White),
                contentPadding = PaddingValues(0.dp),
                elevation = ButtonDefaults.elevation(defaultElevation = 0.dp, pressedElevation = 2.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(R.string.label_open),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Preview
@Composable
fun AuthenticationButtonPreview() {
    CircularButton(
        icon = Icons.Default.Login
    )
}