package pt.ipc_app

import org.junit.Assert.*
import org.junit.Test
import pt.ipc_app.ble.SensorBleParser

class SensorBleParserTests {
    @Test fun parsesDualSensorFirmware() {
        val sample = SensorBleParser.parse("P:-2.0|R:61.5|A:61.5|N:3|V:8.2|S:SET COMPLETE\r\n")!!
        assertEquals(3, sample.reps)
        assertEquals(61.5f, sample.angle, 0.01f)
        assertEquals("SET COMPLETE", sample.status)
    }
    @Test fun supportsPreviousCompactProtocol() {
        val sample = SensorBleParser.parse("P:2|R:3|N:4|V:5|S:HOLD")!!
        assertEquals(4, sample.reps)
        assertEquals(3f, sample.angle, 0f)
    }
    @Test fun rejectsPartialAndControlMessages() {
        assertNull(SensorBleParser.parse("P:2|R:3|A:"))
        assertNull(SensorBleParser.parse("SESSION STARTED"))
        assertNull(SensorBleParser.parse("P:1..2|R:3|N:1|V:2|S:HOLD"))
    }
    @Test fun keepsRawPitchSeparateFromDirectionCorrectedExerciseAngle() {
        val sample = SensorBleParser.parse("P:-70|R:4|A:70|N:0|V:0|S:READY")!!
        assertEquals(-70f, sample.pitch, 0f)
        assertEquals(4f, sample.roll, 0f)
        assertEquals(70f, sample.angle, 0f)
        assertEquals(0, sample.reps)
        assertEquals("READY", sample.status)
    }
}
