package pt.ipc_app.ui.screens.exercises.selection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ipc_app.R
import pt.ipc_app.ui.components.ScreenHeader
import pt.ipc_app.ui.theme.*

@Composable
fun ChooseCameraOrSensorScreen(
    onCameraSelected: () -> Unit,
    onSensorSelected: () -> Unit,
    cameraEnabled: Boolean = false,
    sensorsEnabled: Boolean = false,
    loading: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        ScreenHeader(
            title = stringResource(R.string.choose_detection_method),
            subtitle = stringResource(R.string.choose_detection_subtitle),
            titleAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Spacer(modifier = Modifier.weight(0.3f))

            // Camera Option Card
            OptionCard(
                title = stringResource(R.string.use_camera),
                description = stringResource(if (loading) R.string.modality_loading else if (cameraEnabled) R.string.use_camera_desc else R.string.modality_unavailable),
                enabled = cameraEnabled,
                icon = Icons.Default.Camera,
                iconColor = MediumBlue,
                onClick = onCameraSelected,
                modifier = Modifier.fillMaxWidth()
            )

            // Sensor Option Card
            OptionCard(
                title = stringResource(R.string.use_sensors),
                description = stringResource(if (loading) R.string.modality_loading else if (sensorsEnabled) R.string.use_sensors_desc else R.string.modality_unavailable),
                enabled = sensorsEnabled,
                icon = Icons.Default.Sensors,
                iconColor = SuccessGreen,
                onClick = onSensorSelected,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(0.3f))
        }
    }
}

@Composable
fun OptionCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Card(
        modifier = modifier.alpha(if (enabled) 1f else 0.45f).clickable(enabled = enabled, onClick = onClick),
        elevation = 1.dp,
        shape = CardShape,
        backgroundColor = SurfaceLight
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon Container
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        iconColor.copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Text Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = MediumBlue
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.body2,
                        color = MediumGrey,
                    fontSize = 13.sp
                )
            }

            // Arrow indicator
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(R.string.cd_select),
                tint = MediumBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview
@Composable
fun ChooseCameraOrSensorScreenPreview() {
    ChooseCameraOrSensorScreen(
        onCameraSelected = {},
        onSensorSelected = {}
    )
}
