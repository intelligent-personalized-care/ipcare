package pt.ipc.api

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import pt.ipc.domain.exercises.*
import java.util.UUID

class SensorValidationTests {
    private val sample = SensorSample(100, 0f, 60f, 4f)
    private val valid = SensorSessionInput(UUID.randomUUID(), 1, 10, 1000, listOf(sample))

    @Test fun `accepts valid sensor session`() { valid.validate() }

    @Test fun `rejects unordered samples`() {
        assertThrows(InvalidSensorSession::class.java) { valid.copy(samples = listOf(sample, sample.copy(elapsedMs = 0))).validate() }
    }

    @Test fun `rejects samples outside session duration`() {
        assertThrows(InvalidSensorSession::class.java) { valid.copy(durationMs = 50).validate() }
    }

    @Test fun `rejects non finite and oversized input`() {
        assertThrows(InvalidSensorSession::class.java) { valid.copy(samples = listOf(sample.copy(roll = Float.NaN))).validate() }
        assertThrows(InvalidSensorSession::class.java) { valid.copy(samples = List(2001) { sample }).validate() }
    }

    @Test fun `rejects unreachable movement thresholds`() {
        assertThrows(InvalidSensorSession::class.java) {
            SensorProfile(60f, 25f, 300, 500, 800, 6f, -35f, 35f, -10f, 45f).validate()
        }
    }
}
