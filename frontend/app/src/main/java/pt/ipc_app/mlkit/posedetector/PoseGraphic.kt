/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pt.ipc_app.mlkit.posedetector

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import pt.ipc_app.mlkit.GraphicOverlay

/** Rendering only. Repainting the view must never count another repetition. */
class PoseGraphic(
    overlay: GraphicOverlay,
    private val pose: Pose,
    private val selected: List<Int>,
    private val measurement: CameraMeasurement
) : GraphicOverlay.Graphic(overlay) {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { strokeWidth = 5f; strokeCap = Paint.Cap.ROUND }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 42f; setShadowLayer(5f, 0f, 1f, Color.BLACK) }
    override fun draw(canvas: Canvas) {
        val body = listOf(11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16, 11 to 23, 12 to 24,
            23 to 24, 23 to 25, 25 to 27, 24 to 26, 26 to 28)
        stroke.color = Color.argb(140, 230, 239, 240); stroke.strokeWidth = 4f
        body.forEach { (a, b) -> line(canvas, a, b) }
        stroke.color = when (measurement.phase) {
            CameraPhase.LIMIT, CameraPhase.TRACKING_LOST -> Color.rgb(255, 170, 80)
            CameraPhase.HOLD, CameraPhase.RETURN, CameraPhase.COMPLETE -> Color.rgb(72, 224, 171)
            else -> Color.rgb(78, 210, 234)
        }
        stroke.strokeWidth = 9f
        selected.zipWithNext().forEach { (a, b) -> line(canvas, a, b) }
        selected.mapNotNull { pose.getPoseLandmark(it) }.filter { it.inFrameLikelihood >= .8f }.forEach {
            canvas.drawCircle(translateX(it.position.x), translateY(it.position.y), 10f, stroke)
        }
        val centre = selected.getOrNull(1)?.let { pose.getPoseLandmark(it) }
        if (centre != null && measurement.angle != null) {
            canvas.drawText("${measurement.angle.toInt()}°", translateX(centre.position.x) + 20f,
                translateY(centre.position.y) - 22f, label)
        }
    }
    private fun line(canvas: Canvas, a: Int, b: Int) {
        val first = pose.getPoseLandmark(a) ?: return
        val second = pose.getPoseLandmark(b) ?: return
        if (first.inFrameLikelihood < .8f || second.inFrameLikelihood < .8f) return
        canvas.drawLine(translateX(first.position.x), translateY(first.position.y),
            translateX(second.position.x), translateY(second.position.y), stroke)
    }
}

fun CameraJoint.landmarks(side: BodySide): List<Int> = when (this) {
    CameraJoint.WRIST -> if (side == BodySide.LEFT) listOf(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST, PoseLandmark.LEFT_INDEX)
        else listOf(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST, PoseLandmark.RIGHT_INDEX)
    CameraJoint.ELBOW -> if (side == BodySide.LEFT) listOf(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        else listOf(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
    CameraJoint.KNEE -> if (side == BodySide.LEFT) listOf(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
        else listOf(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
}
