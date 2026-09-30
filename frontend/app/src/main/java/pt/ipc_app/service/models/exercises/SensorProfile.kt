package pt.ipc_app.service.models.exercises

data class SensorProfile(
    val raiseThreshold: Float,
    val lowerThreshold: Float,
    val minRaiseTimeMs: Int,
    val holdTimeMs: Int,
    val cooldownMs: Int,
    val minMovementSpeed: Float,
    val minPitch: Float,
    val maxPitch: Float,
    val minRoll: Float,
    val maxRoll: Float,
    val useRoll: Boolean = true,
    val movementDirection: Int = 1
) {
    fun isValid(): Boolean = listOf(raiseThreshold, lowerThreshold, minMovementSpeed, minPitch, maxPitch, minRoll, maxRoll).all { it.isFinite() } &&
        movementDirection in listOf(-1, 1) && minRaiseTimeMs in 0..60000 && holdTimeMs in 0..60000 && cooldownMs in 0..60000 && minMovementSpeed >= 0 &&
        minPitch >= -180 && maxPitch <= 180 && minPitch < maxPitch && minRoll >= -180 && maxRoll <= 180 && minRoll < maxRoll &&
        raiseThreshold > lowerThreshold && lowerThreshold >= (if (useRoll) minRoll else minPitch) && raiseThreshold <= (if (useRoll) maxRoll else maxPitch)

    fun command(reps: Int): String = "START2:" + listOf(reps, raiseThreshold, lowerThreshold,
        minRaiseTimeMs, holdTimeMs, cooldownMs, minMovementSpeed, minPitch, maxPitch, minRoll, maxRoll,
        if (useRoll) 1 else 0, movementDirection).joinToString(",")
}

data class SensorProgress(val completedSets: List<Int>)
