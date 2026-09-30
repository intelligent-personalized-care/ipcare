package pt.ipc.domain.exercises

import pt.ipc.domain.exceptions.BadRequest
import java.util.UUID

class InvalidSensorSession(message: String) : BadRequest(message)

data class SensorSample(
    val elapsedMs: Long,
    val pitch: Float,
    val roll: Float,
    val velocity: Float
)

data class SensorSessionInput(
    val sessionId: UUID,
    val set: Int,
    val repetitions: Int,
    val durationMs: Long,
    val samples: List<SensorSample>,
    val profile: SensorProfile? = null,
    val withLoad: Boolean? = null,
    val loadValue: Float? = null,
    val loadUnit: String? = null
) {
    fun validate() {
        profile?.validate()
        validateExecutionLoad(withLoad, loadValue, loadUnit)
        if (set !in 1..20 || repetitions !in 1..200 || durationMs !in 1..86_400_000L) {
            throw InvalidSensorSession("Invalid set, repetitions or duration")
        }
        if (samples.isEmpty() || samples.size > 2000) throw InvalidSensorSession("Expected 1 to 2000 sensor samples")
        if (samples.any {
            it.elapsedMs !in 0..durationMs || !it.pitch.isFinite() || !it.roll.isFinite() ||
                !it.velocity.isFinite() || it.pitch !in -180f..180f || it.roll !in -180f..180f
        } || samples.zipWithNext().any { (a, b) -> a.elapsedMs > b.elapsedMs }
        ) {
            throw InvalidSensorSession("Invalid sensor samples")
        }
    }
}
