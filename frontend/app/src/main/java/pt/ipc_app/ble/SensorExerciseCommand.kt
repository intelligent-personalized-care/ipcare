package pt.ipc_app.ble

import pt.ipc_app.service.models.exercises.SensorProfile

/** Single UTF-8 GATT write; delimiters/control characters cannot become profile fields. */
object SensorExerciseCommand {
    const val MAX_BYTES = 244
    const val MAX_NAME_BYTES = 48
    fun calibration(name: String, profile: SensorProfile, reps: Int): String {
        require(profile.isValid() && reps in 1..200) { "Perfil de exercício inválido." }
        val cleaned = name.map { if (it == ',' || it.isISOControl()) ' ' else it }.joinToString("").trim()
        val label = StringBuilder()
        var bytes = 0
        val points = cleaned.codePoints().toArray()
        for (point in points) {
            val character = String(Character.toChars(point))
            val size = character.toByteArray(Charsets.UTF_8).size
            if (bytes + size > MAX_NAME_BYTES) break
            label.append(character); bytes += size
        }
        val title = label.toString().trim().ifEmpty { "Exercise" }
        return ("CALIBRATE2:$title," + profile.command(reps).removePrefix("START2:")).also {
            require(it.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "O perfil excede o tamanho do pacote BLE." }
        }
    }
}
