package pt.ipc_app.ui.screens.exercises.sensor

import androidx.compose.ui.res.stringResource

import pt.ipc_app.R

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pt.ipc_app.ui.theme.*

@Composable
internal fun SensorPlacementCard(exerciseTitle: String, description: String) {
    val joint = sensorJoint(exerciseTitle)
    val startPosition = sensorStartPosition(exerciseTitle)?.let { stringResource(it) }
    var showDescription by rememberSaveable(exerciseTitle) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth(), shape = CardShape, backgroundColor = SurfaceLight) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.feedback_one_strap_on_each_side_of_the_joint), style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            PlacementIllustration(joint)
            Text(stringResource(R.string.feedback_1_main_module), color = MediumBlue, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.placement_main, stringResource(joint.proximal)), style = MaterialTheme.typography.body2)
            Text(stringResource(R.string.feedback_2_smaller_module), color = SuccessGreen, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.placement_secondary, stringResource(joint.distal)), style = MaterialTheme.typography.body2)
            TextButton(onClick = { showDescription = !showDescription }) { Text(if (showDescription) stringResource(R.string.feedback_less_information) else stringResource(R.string.feedback_view_placement_instructions)) }
            if (showDescription) {
                Text(stringResource(R.string.feedback_faces_outward_modules_aligned_and_cable_slack_keep_the_joint_unob))
                Text(stringResource(R.string.feedback_the_illustration_shows_placement_not_the_calibration_posture), style = MaterialTheme.typography.caption)
                Text(stringResource(R.string.feedback_ttp223_touch_for_1_5_s_and_release_to_recalibrate_or_for_4_s_and), style = MaterialTheme.typography.caption)
                (startPosition ?: description.takeIf { it.isNotBlank() })?.let { Text(it) }
                if (startPosition != null && description.isNotBlank()) Text(description, style = MaterialTheme.typography.body2)
            }
        }
    }
}

/** Vector illustration stays sharp on different screen sizes; labels remain accessible text. */
@Composable
private fun PlacementIllustration(joint: SensorJoint) {
    val description = stringResource(R.string.placement_description, stringResource(joint.proximal), stringResource(joint.distal))
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.65f).semantics { contentDescription = description }) {
        withTransform({ scale(size.width / 360f, size.height / 220f, Offset.Zero) }) {
            val skin = Color(0xFFE5C9B6)
            val outline = Color(0xFF95735E)
            val strap = Color(0xFF718096)
            fun limb(a: Offset, b: Offset, width: Float) {
                drawLine(outline, a, b, width + 3, StrokeCap.Round)
                drawLine(skin, a, b, width, StrokeCap.Round)
            }
            val large: Offset
            val small: Offset
            when (joint) {
                SensorJoint.WRIST -> {
                    limb(Offset(24f, 128f), Offset(220f, 128f), 58f)
                    limb(Offset(243f, 128f), Offset(286f, 128f), 65f)
                    for (i in 0..3) limb(Offset(277f, 105f + i * 15), Offset(329f - kotlin.math.abs(1 - i) * 9, 105f + i * 15), 11f)
                    limb(Offset(253f, 146f), Offset(272f, 178f), 16f)
                    drawLine(outline, Offset(215f, 105f), Offset(215f, 151f), 2f)
                    large = Offset(130f, 128f); small = Offset(260f, 128f)
                }
                SensorJoint.ELBOW -> {
                    limb(Offset(64f, 35f), Offset(105f, 155f), 55f)
                    limb(Offset(105f, 155f), Offset(278f, 149f), 43f)
                    limb(Offset(295f, 149f), Offset(325f, 142f), 31f)
                    drawCircle(outline.copy(alpha = .4f), 15f, Offset(105f, 155f), style = Stroke(2f))
                    large = Offset(77f, 77f); small = Offset(210f, 150f)
                }
                SensorJoint.KNEE -> {
                    limb(Offset(36f, 63f), Offset(215f, 70f), 67f)
                    limb(Offset(215f, 70f), Offset(235f, 171f), 45f)
                    limb(Offset(235f, 185f), Offset(301f, 188f), 29f)
                    drawCircle(outline.copy(alpha = .4f), 19f, Offset(218f, 72f), style = Stroke(2f))
                    large = Offset(115f, 65f); small = Offset(228f, 139f)
                }
                SensorJoint.UNKNOWN -> {
                    limb(Offset(40f, 128f), Offset(310f, 128f), 48f)
                    drawCircle(outline, 13f, Offset(180f, 128f), style = Stroke(2f))
                    large = Offset(105f, 128f); small = Offset(258f, 128f)
                }
            }
            val wire = Path().apply {
                moveTo(large.x + 22, large.y)
                cubicTo(large.x + 75, large.y - 65, small.x - 50, small.y - 55, small.x, small.y)
            }
            drawPath(wire, Color(0xFF475569), style = Stroke(3f))
            fun module(center: Offset, width: Float, height: Float, color: Color, number: String) {
                drawRoundRect(strap, Offset(center.x - 8, center.y - 36), Size(16f, 72f), CornerRadius(5f))
                drawRoundRect(Color.White, Offset(center.x - width / 2 - 2, center.y - height / 2 - 2), Size(width + 4, height + 4), CornerRadius(7f))
                drawRoundRect(color, Offset(center.x - width / 2, center.y - height / 2), Size(width, height), CornerRadius(6f))
                drawRoundRect(Color(0xFFD9E3EB), Offset(center.x - 9, center.y - 7), Size(18f, 14f), CornerRadius(2f))
                val badge = Offset(center.x, (center.y - 55).coerceAtLeast(18f))
                drawCircle(color, 15f, badge)
                drawContext.canvas.nativeCanvas.drawText(number, badge.x, badge.y + 5,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE; textSize = 17f; textAlign = Paint.Align.CENTER; isFakeBoldText = true })
            }
            module(large, 49f, 35f, MediumBlue, "1")
            module(small, 27f, 24f, SuccessGreen, "2")
        }
    }
}
