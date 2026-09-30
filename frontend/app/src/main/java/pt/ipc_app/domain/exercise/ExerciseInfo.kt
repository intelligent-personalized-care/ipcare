package pt.ipc_app.domain.exercise

import java.util.*

data class ExerciseInfo(
    val id: UUID,
    val title: String,
    val description: String,
    val type: ExerciseType,
    val supportsCamera: Boolean? = null,
    val supportsSensors: Boolean? = null,
    val raiseThreshold: Float? = null,
    val lowerThreshold: Float? = null,
    val cooldownMs: Int? = null,
    val minRaiseTimeMs: Int? = null,
    val holdTimeMs: Int? = null,
    val minMovementSpeed: Float? = null,
    val maxPitch: Float? = null,
    val minPitch: Float? = null,
    val maxRoll: Float? = null,
    val minRoll: Float? = null,
    val useRoll: Boolean? = null,
    val movementDirection: Int? = null,
    val cameraJoint: String? = null,
    val cameraMovement: String? = null
) {
    fun sensorProfile(): pt.ipc_app.service.models.exercises.SensorProfile {
        require(supportsSensors == true) { "Este exercício não tem suporte de sensores." }
        return movementProfile()
    }

    fun movementProfile(): pt.ipc_app.service.models.exercises.SensorProfile {
        return pt.ipc_app.service.models.exercises.SensorProfile(
            requireNotNull(raiseThreshold), requireNotNull(lowerThreshold), requireNotNull(minRaiseTimeMs),
            requireNotNull(holdTimeMs), requireNotNull(cooldownMs), requireNotNull(minMovementSpeed),
            requireNotNull(minPitch), requireNotNull(maxPitch), requireNotNull(minRoll), requireNotNull(maxRoll),
            requireNotNull(useRoll), requireNotNull(movementDirection))
    }
}

enum class ExerciseType {
    Legs,
    Byceps,
    Shoulders,
    Back,
    Gluts,
    Abdominals,
    Chest,
    Forearms,
    Traps,
    Biceps,
    Triceps
}
