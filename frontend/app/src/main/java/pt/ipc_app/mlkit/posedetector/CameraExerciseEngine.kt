package pt.ipc_app.mlkit.posedetector

import pt.ipc_app.service.models.exercises.SensorProfile
import kotlin.math.*

enum class CameraJoint { WRIST, ELBOW, KNEE }
enum class CameraMovement { FLEXION, EXTENSION }
enum class BodySide { LEFT, RIGHT }
data class PosePoint(val x: Float, val y: Float, val confidence: Float)
data class JointObservation(val proximal: PosePoint, val joint: PosePoint, val distal: PosePoint)

/** Image-space geometry, before preview mirroring. Z is deliberately not treated as metric depth. */
object CameraAngles {
    fun measure(kind: CameraJoint, observation: JointObservation): Float? {
        val (a, b, c) = observation
        if (listOf(a, b, c).any { !it.x.isFinite() || !it.y.isFinite() || !it.confidence.isFinite() || it.confidence < .8f }) return null
        val ux = b.x - a.x; val uy = b.y - a.y
        val vx = c.x - b.x; val vy = c.y - b.y
        val lengthA = hypot(ux, uy); val lengthB = hypot(vx, vy)
        if (lengthA < 24f || lengthB < if (kind == CameraJoint.WRIST) 12f else 24f) return null
        if (kind == CameraJoint.WRIST) {
            // Palm down, forearm horizontal and camera at hand height: downward = flexion.
            // Using the forearm's horizontal direction also handles left-facing and mirrored poses.
            if (abs(ux) / lengthA < .85f) return null
            return Math.toDegrees(atan2((ux * vy - uy * vx).toDouble(), (ux * vx + uy * vy).toDouble())).toFloat() * sign(ux)
        }
        // Deflection of consecutive segments: zero is an extended elbow/knee.
        return Math.toDegrees(acos(((ux * vx + uy * vy) / (lengthA * lengthB)).coerceIn(-1f, 1f).toDouble())).toFloat()
    }
}

enum class CameraPhase { POSITION, CALIBRATING, READY, MOVE, HOLD, RETURN, COMPLETE, TRACKING_LOST, LIMIT }
data class CameraMeasurement(
    val angle: Float? = null,
    val baseline: Float? = null,
    val phase: CameraPhase = CameraPhase.POSITION,
    val repetitions: Int = 0,
    val holdProgress: Float = 0f,
    val calibrationProgress: Float = 0f,
    val calibrated: Boolean = false
)

class CameraExerciseEngine(
    val profile: SensorProfile,
    private val joint: CameraJoint,
    private val movement: CameraMovement,
    private val targetReps: Int
) {
    init { require(profile.isValid() && targetReps > 0 && profile.lowerThreshold >= 0 && profile.raiseThreshold > 0) }
    private var baseline: Float? = null
    private var calibrating = false
    private val calibration = mutableListOf<Pair<Long, Float>>()
    private var previousAt: Long? = null
    private var filtered: Float? = null
    private var active = false
    private var armed = false
    private var raisedAt: Long? = null
    private var holdAt: Long? = null
    private var returning = false
    private var lastRepAt: Long? = null
    private var reps = 0
    var measurement = CameraMeasurement(); private set

    fun calibrate() {
        active = false; baseline = null; calibrating = true; calibration.clear()
        previousAt = null; filtered = null; resetCycle()
        measurement = CameraMeasurement(phase = CameraPhase.CALIBRATING)
    }

    fun invalidate() {
        baseline = null; calibrating = false; active = false; calibration.clear()
        previousAt = null; filtered = null; resetCycle()
        measurement = CameraMeasurement(repetitions = reps)
    }

    fun canStart(): Boolean = measurement.calibrated && measurement.phase == CameraPhase.READY &&
        measurement.angle?.let { it in 0f.coerceAtMost(minimum)..profile.lowerThreshold } == true

    fun start(): Boolean {
        if (!canStart()) return false
        active = true; reps = 0; lastRepAt = null; resetCycle()
        measurement = measurement.copy(repetitions = 0, phase = CameraPhase.MOVE, holdProgress = 0f)
        return true
    }

    fun stop() { active = false; resetCycle() }
    private val minimum get() = if (profile.useRoll) profile.minRoll else profile.minPitch
    private val maximum get() = if (profile.useRoll) profile.maxRoll else profile.maxPitch
    private fun resetCycle() { armed = false; raisedAt = null; holdAt = null; returning = false }

    fun update(rawAngle: Float?, at: Long): CameraMeasurement {
        val last = previousAt
        if (last != null && at <= last) return measurement // reject stale/reordered frames
        val gap = last != null && at - last > 250
        previousAt = at
        if (gap) { resetCycle(); filtered = null; calibration.clear() }
        if (rawAngle == null || !rawAngle.isFinite()) {
            resetCycle(); filtered = null; calibration.clear()
            return publish(null, CameraPhase.TRACKING_LOST)
        }
        if (calibrating) {
            calibration.add(at to rawAngle)
            // Sliding window: tolerate detection jitter and isolated outliers without accepting a moving limb.
            while (calibration.size > 1 && at - calibration[1].first >= 1500) calibration.removeAt(0)
            val elapsed = at - calibration.first().first
            val values = calibration.map { it.second }.sorted()
            val trim = values.size / 10
            val stable = values[values.lastIndex - trim] - values[trim] <= 10f
            fun median(samples: List<Pair<Long, Float>>) = samples.map { it.second }.sorted().let { it[it.size / 2] }
            val third = (calibration.size / 3).coerceAtLeast(1)
            val drift = kotlin.math.abs(median(calibration.take(third)) - median(calibration.takeLast(third)))
            if (elapsed >= 1500 && calibration.size >= 15 && stable && drift <= 5f) {
                val value = values[values.size / 2]
                // Reject a posture from which the requested excursion cannot be reached.
                val reachable = when {
                    joint == CameraJoint.WRIST -> abs(value) <= 15f && profile.raiseThreshold <= 90f
                    movement == CameraMovement.FLEXION -> value + profile.raiseThreshold <= 180f
                    else -> value >= profile.raiseThreshold
                }
                if (!reachable) { calibration.clear(); return publish(null, CameraPhase.LIMIT) }
                baseline = value; calibrating = false; filtered = 0f
                return publish(0f, CameraPhase.READY)
            }
            measurement = CameraMeasurement(phase = CameraPhase.CALIBRATING, calibrationProgress = if (stable && drift <= 5f) (elapsed / 1500f).coerceIn(0f, 1f) else 0f)
            return measurement
        }
        val zero = baseline ?: return publish(null, CameraPhase.POSITION)
        val rawExcursion = (rawAngle - zero) * if (movement == CameraMovement.FLEXION) 1 else -1
        if (rawExcursion < minimum || rawExcursion > maximum) {
            resetCycle(); filtered = null
            return publish(rawExcursion, CameraPhase.LIMIT)
        }
        val old = filtered
        val dt = last?.let { at - it } ?: 0
        val alpha = if (dt > 0) (1 - exp(-dt / 100.0)).toFloat() else 1f
        val angle = old?.let { it + alpha * (rawExcursion - it) } ?: rawExcursion
        filtered = angle
        val speed = if (old != null && dt > 0) (angle - old) * 1000f / dt else 0f
        if (!active) return publish(angle, if (reps >= targetReps) CameraPhase.COMPLETE else CameraPhase.READY)
        if (!armed) {
            if (angle <= profile.lowerThreshold && (lastRepAt == null || at - lastRepAt!! >= profile.cooldownMs)) armed = true
            return publish(angle, if (armed) CameraPhase.MOVE else CameraPhase.RETURN)
        }
        if (returning) {
            if (angle <= profile.lowerThreshold) {
                reps++; lastRepAt = at; resetCycle()
                if (reps >= targetReps) { active = false; return publish(angle, CameraPhase.COMPLETE) }
            }
            return publish(angle, CameraPhase.RETURN)
        }
        if (angle <= profile.lowerThreshold) { raisedAt = null; holdAt = null }
        else if (raisedAt == null && speed >= profile.minMovementSpeed) raisedAt = at
        if (angle >= profile.raiseThreshold && raisedAt != null) {
            if (at - raisedAt!! < profile.minRaiseTimeMs) {
                // A fast raise cannot become valid merely by waiting at the target.
                resetCycle(); return publish(angle, CameraPhase.RETURN)
            }
            if (holdAt == null) holdAt = at
            val progress = if (profile.holdTimeMs == 0) 1f else ((at - holdAt!!) / profile.holdTimeMs.toFloat()).coerceIn(0f, 1f)
            if (progress >= 1f) returning = true
            return publish(angle, if (returning) CameraPhase.RETURN else CameraPhase.HOLD, progress)
        }
        // Holding time must be continuous above the prescribed target.
        holdAt = null
        return publish(angle, CameraPhase.MOVE)
    }

    private fun publish(angle: Float?, phase: CameraPhase, hold: Float = 0f): CameraMeasurement {
        measurement = CameraMeasurement(angle, baseline, phase, reps, hold, calibrated = baseline != null)
        return measurement
    }
}
