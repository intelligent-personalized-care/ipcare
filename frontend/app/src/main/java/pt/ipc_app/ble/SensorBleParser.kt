package pt.ipc_app.ble

/**
 * Parses lines from the ESP32 prototype over BLE.
 *
 * Compact (BLE): P:12.3|R:45.6|N:3|V:8.2|S:status text
 * Legacy: P:12.3° | R:45.6° | Reps:3 | Vel:8.2°/s | status
 */
data class ParsedSensorLine(
    val pitch: Float,
    val roll: Float,
    val reps: Int,
    val velocity: Float,
    val status: String,
    val angle: Float = roll
)

object SensorBleParser {

    private val compactRegex = Regex(
        """^P:([\d.\-+]+)\|R:([\d.\-+]+)(?:\|A:([\d.\-+]+))?\|N:(\d+)\|V:([\d.\-+]+)\|S:(.*)$"""
    )

    private val legacyPitch = Regex("""P:([\d.\-+]+)""")
    private val legacyRoll = Regex("""R:([\d.\-+]+)""")
    private val legacyReps = Regex("""Reps:\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val legacyVel = Regex("""Vel:([\d.\-+]+)""", RegexOption.IGNORE_CASE)

    fun parse(line: String): ParsedSensorLine? {
        val trimmed = line.trim().trimEnd('\r', '\n', '\u0000')
        if (trimmed.isEmpty()) return null

        compactRegex.matchEntire(trimmed)?.let { m ->
            val pitch = m.groupValues[1].toFloatOrNull() ?: return null
            val roll = m.groupValues[2].toFloatOrNull() ?: return null
            val angle = m.groupValues[3].takeIf { it.isNotEmpty() }?.toFloatOrNull() ?: roll
            val reps = m.groupValues[4].toIntOrNull() ?: return null
            val vel = m.groupValues[5].toFloatOrNull() ?: return null
            val status = m.groupValues[6].trim()
            if (!pitch.isFinite() || !roll.isFinite() || !vel.isFinite() || !angle.isFinite()) return null
            return ParsedSensorLine(pitch, roll, reps, vel, status, angle)
        }

        if (!trimmed.contains("P:") || !trimmed.contains("R:")) return null
        val pitch = legacyPitch.find(trimmed)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        val roll = legacyRoll.find(trimmed)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        val reps = legacyReps.find(trimmed)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val vel = legacyVel.find(trimmed)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
        if (!pitch.isFinite() || !roll.isFinite() || !vel.isFinite()) return null

        val status = trimmed.substringAfterLast("|", trimmed).trim()
        return ParsedSensorLine(pitch, roll, reps, vel, status)
    }
}
