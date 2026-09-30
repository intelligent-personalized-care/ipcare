package pt.ipc_app

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import pt.ipc_app.domain.exercise.ExerciseInfo
import java.io.File

class SensorProfileContractTests {
    // Exercise the real SQL catalogue -> JSON model -> BLE serialization contract.
    @Test fun selectedCatalogueProfilesReachFirmwareWithoutElbowFallback() {
        val seed = File("../../backend/postgresql/insertTable.sql").readText()
        val entries = seed.lines().filter { it.trim().startsWith("('73a20000-") }
        assertEquals(8, entries.size)
        val commands = entries.filter { it.contains("TRUE, TRUE, ") }.map { line ->
            val id = line.substringAfter("('").substringBefore("'")
            val title = line.substringAfter("', '").substringBefore("'")
            val raw = line.substringAfter("TRUE, TRUE, ").substringBefore(")").split(",").map { it.trim() }
            val keys = listOf("useRoll", "movementDirection", "raiseThreshold", "lowerThreshold", "minRaiseTimeMs",
                "holdTimeMs", "cooldownMs", "minMovementSpeed", "minPitch", "maxPitch", "minRoll", "maxRoll")
            val json = """{"id":"$id","title":"$title","description":"Test","type":"Forearms","supportsSensors":true,""" +
                keys.zip(raw).joinToString(",") { (k, v) -> "\"$k\":${v.lowercase()}" } + "}"
            val profile = Gson().fromJson(json, ExerciseInfo::class.java).sensorProfile()
            val command = profile.command(10)
            assertTrue(command.startsWith("START2:10,"))
            assertEquals(13, command.substringAfter(":").split(",").size)
            title to command
        }.toMap()
        assertNotEquals(commands["Wrist Flexion"], commands["Wrist Extension"])
        assertNotEquals(commands["Elbow Flexion"], commands["Knee Extension - Seated"])
        assertTrue(commands.getValue("Wrist Extension").endsWith(",0,-1"))
    }

    @Test fun missingCatalogueConfigurationIsRejected() {
        val entry = Gson().fromJson("""{"id":"73a20000-cc31-4c29-9000-000000000001","title":"Unknown","description":"None","type":"Forearms","supportsSensors":true}""", ExerciseInfo::class.java)
        assertThrows(IllegalArgumentException::class.java) { entry.sensorProfile() }
    }
}
