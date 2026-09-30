package pt.ipc_app

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import pt.ipc_app.mlkit.posedetector.*
import pt.ipc_app.service.models.exercises.SensorProfile
import kotlin.math.cos
import kotlin.math.sin

/** Synthetic landmarks integrate geometry, calibration, countdown and repetition processing. */
@RunWith(Parameterized::class)
class CameraPipelineIntegrationTests(
    private val joint: CameraJoint,
    private val movement: CameraMovement,
    private val baseline: Float
) {
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0} {1}")
        fun cases() = listOf(
            arrayOf(CameraJoint.WRIST, CameraMovement.FLEXION, 0f),
            arrayOf(CameraJoint.WRIST, CameraMovement.EXTENSION, 0f),
            arrayOf(CameraJoint.ELBOW, CameraMovement.FLEXION, 0f),
            arrayOf(CameraJoint.KNEE, CameraMovement.EXTENSION, 90f)
        )
    }

    @Test fun landmarksProduceOneCompleteRepetitionAfterCalibrationAndAutomaticCountdown() {
        val profile = SensorProfile(60f, 10f, 1000, 1000, 800, 0f, -20f, 20f, -10f, 120f)
        val engine = CameraExerciseEngine(profile, joint, movement, 1)
        val countdown = CameraStartCountdown()
        var now = 0L
        fun frame(degrees: Float) {
            val radians = Math.toRadians(degrees.toDouble())
            val observation = JointObservation(PosePoint(0f, 0f, 1f), PosePoint(100f, 0f, 1f),
                PosePoint(100f + 100f * cos(radians).toFloat(), 100f * sin(radians).toFloat(), 1f))
            engine.update(CameraAngles.measure(joint, observation), now)
            now += 50
        }
        engine.calibrate()
        countdown.arm()
        assertNull(countdown.update(false, now))
        repeat(40) { frame(baseline) }
        assertTrue(engine.measurement.calibrated)
        assertEquals(3, countdown.update(true, now))
        repeat(59) { frame(baseline) }
        assertEquals(1, countdown.update(true, now))
        assertEquals(0, engine.measurement.repetitions)
        frame(baseline)
        assertEquals(0, countdown.update(true, now))
        assertTrue(engine.start())
        countdown.cancel()
        repeat(20) { frame(baseline) }
        val sign = if (movement == CameraMovement.FLEXION) 1 else -1
        for (step in 1..60) frame(baseline + sign * 70f * step / 60)
        repeat(40) { frame(baseline + sign * 70f) }
        assertEquals(CameraPhase.RETURN, engine.measurement.phase)
        assertEquals(0, engine.measurement.repetitions)
        for (step in 1..60) frame(baseline + sign * 70f * (60 - step) / 60)
        repeat(40) { frame(baseline) }
        assertEquals(1, engine.measurement.repetitions)
        assertEquals(CameraPhase.COMPLETE, engine.measurement.phase)
    }
}
