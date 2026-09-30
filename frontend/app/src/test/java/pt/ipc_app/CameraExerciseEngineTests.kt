package pt.ipc_app

import org.junit.Assert.*
import org.junit.Test
import pt.ipc_app.mlkit.posedetector.*
import pt.ipc_app.service.models.exercises.SensorProfile

class CameraExerciseEngineTests {
    private val profile = SensorProfile(60f, 10f, 1000, 1000, 800, 0f, -25f, 25f, -10f, 120f)
    private class Run(val engine: CameraExerciseEngine) {
        var now = 0L
        fun frame(angle: Float?) = engine.update(angle, now.also { now += 50 })
        fun stay(angle: Float?, frames: Int = 40) { repeat(frames) { frame(angle) } }
        fun ramp(from: Float, to: Float) { for (i in 1..60) frame(from + (to - from) * i / 60) }
        fun calibrate(angle: Float) { engine.calibrate(); stay(angle); assertTrue(engine.measurement.calibrated); assertTrue(engine.start()); stay(angle, 2) }
    }
    private fun run(movement: CameraMovement = CameraMovement.FLEXION, p: SensorProfile = profile) = Run(CameraExerciseEngine(p, CameraJoint.ELBOW, movement, 2))

    @Test fun countsOnlyAfterTargetContinuousHoldAndReturn() {
        val r = run(); r.calibrate(0f)
        r.ramp(0f, 70f); r.stay(70f)
        assertEquals(0, r.engine.measurement.repetitions)
        assertEquals(CameraPhase.RETURN, r.engine.measurement.phase)
        r.ramp(70f, 0f); r.stay(0f)
        assertEquals(1, r.engine.measurement.repetitions)
        r.ramp(0f, 70f); r.stay(70f); r.ramp(70f, 0f); r.stay(0f)
        assertEquals(2, r.engine.measurement.repetitions)
        assertEquals(CameraPhase.COMPLETE, r.engine.measurement.phase)
        r.ramp(0f, 70f); r.stay(70f); r.ramp(70f, 0f)
        assertEquals(2, r.engine.measurement.repetitions)
    }
    @Test fun extensionUsesDecreaseFromCalibratedAngle() {
        val r = run(CameraMovement.EXTENSION); r.calibrate(90f)
        r.ramp(90f, 20f); r.stay(20f)
        assertEquals(70f, r.engine.measurement.angle!!, 1f)
        r.ramp(20f, 90f); r.stay(90f)
        assertEquals(1, r.engine.measurement.repetitions)
    }
    @Test fun wrongDirectionAndOutOfRangeNeverCount() {
        val r = run(); r.calibrate(0f)
        r.ramp(0f, -30f); r.stay(-30f); assertEquals(CameraPhase.LIMIT, r.engine.measurement.phase)
        r.ramp(-30f, 0f); r.stay(0f); assertEquals(0, r.engine.measurement.repetitions)
        r.ramp(0f, 140f); r.stay(140f); r.ramp(140f, 0f); r.stay(0f)
        assertEquals(0, r.engine.measurement.repetitions)
    }
    @Test fun lostLandmarksInvalidateTheCurrentRepetition() {
        val r = run(); r.calibrate(0f); r.ramp(0f, 70f); r.stay(70f)
        r.frame(null); assertEquals(CameraPhase.TRACKING_LOST, r.engine.measurement.phase)
        r.stay(70f); r.ramp(70f, 0f); r.stay(0f)
        assertEquals(0, r.engine.measurement.repetitions)
    }
    @Test fun frameGapDoesNotCountAsHoldingTime() {
        val r = run(); r.calibrate(0f); r.ramp(0f, 70f)
        r.now += 5000; r.stay(70f); r.ramp(70f, 0f); r.stay(0f)
        assertEquals(0, r.engine.measurement.repetitions)
    }
    @Test fun fastRaiseCannotBeValidatedByWaitingAtTarget() {
        val r = run(); r.calibrate(0f); r.stay(80f, 100); r.ramp(80f, 0f); r.stay(0f)
        assertEquals(0, r.engine.measurement.repetitions)
    }
    @Test fun holdMustRestartAfterDroppingBelowTarget() {
        val r = run(p = profile.copy(holdTimeMs = 5000)); r.calibrate(0f)
        r.ramp(0f, 70f); r.stay(70f, 30)
        assertTrue(r.engine.measurement.holdProgress > 0)
        r.stay(30f, 20); assertEquals(0f, r.engine.measurement.holdProgress, 0f)
        r.ramp(30f, 70f); r.stay(70f, 30); r.ramp(70f, 0f); r.stay(0f)
        assertEquals(0, r.engine.measurement.repetitions)
    }
    @Test fun calibrationRequiresStableContinuousVisibleFrames() {
        val r = run(); r.engine.calibrate()
        repeat(40) { r.frame(if (it % 2 == 0) 0f else 20f) }
        assertFalse(r.engine.measurement.calibrated)
        r.stay(0f, 15); r.frame(null); r.stay(0f, 15)
        assertFalse(r.engine.measurement.calibrated)
        r.stay(0f, 30); assertTrue(r.engine.measurement.calibrated)
    }
    @Test fun impossibleExtensionTargetRejectsCalibration() {
        val r = run(CameraMovement.EXTENSION); r.engine.calibrate(); r.stay(20f)
        assertFalse(r.engine.measurement.calibrated); assertFalse(r.engine.start())
    }
    @Test fun catalogueAndCustomThresholdsChangeCounting() {
        val r = run(p = profile.copy(raiseThreshold = 90f)); r.calibrate(0f)
        r.ramp(0f, 70f); r.stay(70f); r.ramp(70f, 0f); r.stay(0f)
        assertEquals(0, r.engine.measurement.repetitions)
        r.ramp(0f, 100f); r.stay(100f); r.ramp(100f, 0f); r.stay(0f)
        assertEquals(1, r.engine.measurement.repetitions)
    }
    @Test fun geometricAnglesAreTranslationScaleAndMirrorInvariant() {
        fun point(x: Float, y: Float) = PosePoint(x, y, 1f)
        val elbow = JointObservation(point(0f, 0f), point(100f, 0f), point(100f, 100f))
        assertEquals(90f, CameraAngles.measure(CameraJoint.ELBOW, elbow)!!, .01f)
        val wrist = JointObservation(point(0f, 100f), point(100f, 100f), point(150f, 150f))
        val mirror = JointObservation(point(300f, 100f), point(200f, 100f), point(150f, 150f))
        assertEquals(45f, CameraAngles.measure(CameraJoint.WRIST, wrist)!!, .01f)
        assertEquals(45f, CameraAngles.measure(CameraJoint.WRIST, mirror)!!, .01f)
        val scaled = JointObservation(point(10f, 210f), point(210f, 210f), point(310f, 310f))
        assertEquals(45f, CameraAngles.measure(CameraJoint.WRIST, scaled)!!, .01f)
        assertEquals(-45f, CameraAngles.measure(CameraJoint.WRIST, wrist.copy(distal = point(150f, 50f)))!!, .01f)
    }
    @Test fun lowConfidenceCollapsedOrVerticalWristGeometryIsRejected() {
        val a = PosePoint(0f, 0f, 1f); val b = PosePoint(0f, 100f, 1f); val c = PosePoint(60f, 160f, 1f)
        assertNull(CameraAngles.measure(CameraJoint.ELBOW, JointObservation(a, b, c.copy(confidence = .5f))))
        assertNull(CameraAngles.measure(CameraJoint.ELBOW, JointObservation(a, a, c)))
        assertNull(CameraAngles.measure(CameraJoint.WRIST, JointObservation(a, b, c)))
    }

    @Test fun calibrationAcceptsStationaryJitterAndIsolatedDetectionOutliers() {
        val r = run(); r.engine.calibrate()
        repeat(31) { r.frame(if (it == 10) 50f else 12f + listOf(-4f, 0f, 4f)[it % 3]) }
        assertTrue(r.engine.measurement.calibrated)
        assertEquals(12f, r.engine.measurement.baseline!!, .01f)
        assertEquals(0, r.engine.measurement.repetitions)
    }
    @Test fun slowMovementCannotBeMistakenForStableCalibration() {
        val r = run(); r.engine.calibrate()
        repeat(65) { r.frame(it * .5f) }
        assertFalse(r.engine.measurement.calibrated)
        r.stay(32f, 40)
        assertTrue(r.engine.measurement.calibrated)
    }

    @Test fun detectionLossPreservesCompletedRepetitionsAndAllowsAnotherCycle() {
        val r = run(); r.calibrate(0f)
        r.ramp(0f, 70f); r.stay(70f); r.ramp(70f, 0f); r.stay(0f)
        assertEquals(1, r.engine.measurement.repetitions)
        r.stay(null, 20)
        assertEquals(1, r.engine.measurement.repetitions)
        assertTrue(r.engine.measurement.calibrated)
        r.stay(0f); r.ramp(0f, 70f); r.stay(70f); r.ramp(70f, 0f); r.stay(0f)
        assertEquals(2, r.engine.measurement.repetitions)
        assertEquals(CameraPhase.COMPLETE, r.engine.measurement.phase)
    }

    @Test fun reorderedFramesCannotAdvanceHoldOrCorruptMeasurement() {
        val r = run(); r.calibrate(0f); r.ramp(0f, 70f)
        val before = r.engine.measurement
        assertEquals(before, r.engine.update(180f, r.now - 50))
        assertEquals(before, r.engine.update(null, r.now - 500))
    }

    @Test fun recalibrationReplacesPreviousBaselineAndClearsPreviousSet() {
        val r = run(); r.calibrate(0f)
        r.ramp(0f, 70f); r.stay(70f); r.ramp(70f, 0f); r.stay(0f)
        assertEquals(1, r.engine.measurement.repetitions)
        r.calibrate(20f)
        assertEquals(20f, r.engine.measurement.baseline!!, .01f)
        assertEquals(0, r.engine.measurement.repetitions)
        r.ramp(20f, 90f); r.stay(90f); r.ramp(90f, 20f); r.stay(20f)
        assertEquals(1, r.engine.measurement.repetitions)
    }
}
