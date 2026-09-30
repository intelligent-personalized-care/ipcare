package pt.ipc.domain.exercises

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
)

enum class ExerciseType {
    Legs,
    Shoulders,
    Back,
    Abdominals,
    Chest,
    Forearms,
    Traps,
    Biceps,
    Triceps
}
