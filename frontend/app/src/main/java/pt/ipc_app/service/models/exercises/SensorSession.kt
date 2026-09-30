package pt.ipc_app.service.models.exercises

import java.util.UUID

data class SensorSample(val elapsedMs: Long, val pitch: Float, val roll: Float, val velocity: Float)

data class SensorSession(
    val sessionId: UUID,
    val set: Int,
    val repetitions: Int,
    val durationMs: Long,
    val samples: List<SensorSample>,
    val profile: SensorProfile? = null,
    val withLoad: Boolean? = null,
    val loadValue: Float? = null,
    val loadUnit: String? = null
)
