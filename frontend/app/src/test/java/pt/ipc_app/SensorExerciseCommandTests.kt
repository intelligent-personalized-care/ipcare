package pt.ipc_app

import org.junit.Assert.*
import org.junit.Test
import pt.ipc_app.ble.SensorExerciseCommand
import pt.ipc_app.service.models.exercises.SensorProfile

class SensorExerciseCommandTests {
    private val profile = SensorProfile(30f, 5f, 1000, 5000, 1000, 0f, -20f, 20f, -10f, 60f)
    @Test fun sendsTheTitleAndAllThirteenNumericFieldsInFirmwareOrder() {
        assertEquals("CALIBRATE2:Wrist Flexion,10,30.0,5.0,1000,5000,1000,0.0,-20.0,20.0,-10.0,60.0,1,1",
            SensorExerciseCommand.calibration("Wrist Flexion", profile, 10))
    }
    @Test fun sanitizesDelimitersAndTruncatesAtUtf8CharacterBoundaries() {
        val command = SensorExerciseCommand.calibration("Flexão,\n" + "ação🦾".repeat(30), profile, 10)
        val title = command.removePrefix("CALIBRATE2:").substringBefore(',')
        assertFalse(title.contains('\n'))
        assertTrue(title.toByteArray(Charsets.UTF_8).size <= 48)
        assertEquals(title, title.toByteArray(Charsets.UTF_8).toString(Charsets.UTF_8))
        assertEquals(14, command.removePrefix("CALIBRATE2:").split(',').size)
        assertTrue(command.toByteArray(Charsets.UTF_8).size <= 244)
    }
    @Test fun preservesAxisDirectionTimingsAndLargeValidValuesWithinOnePacket() {
        val command = SensorExerciseCommand.calibration("x".repeat(80), profile.copy(useRoll = false,
            minPitch = -180f, maxPitch = 180f, movementDirection = -1, minMovementSpeed = Float.MAX_VALUE,
            minRaiseTimeMs = 60000, holdTimeMs = 60000, cooldownMs = 60000), 200)
        assertTrue(command.endsWith(",0,-1"))
        assertTrue(command.toByteArray(Charsets.UTF_8).size <= 244)
    }
    @Test fun rejectsInvalidProfilesBeforeWriting() {
        assertThrows(IllegalArgumentException::class.java) { SensorExerciseCommand.calibration("Elbow", profile.copy(raiseThreshold = Float.NaN), 10) }
        assertThrows(IllegalArgumentException::class.java) { SensorExerciseCommand.calibration("Elbow", profile, 0) }
    }
}
