package pt.ipc.api

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import pt.ipc.domain.exercises.*

class FeedbackScoreTests {
    @Test fun `accepts optional score and each of the five stars`() {
        validateFeedbackScore(null)
        (1..5).forEach { validateFeedbackScore(it.toString()) }
    }
    @Test fun `rejects out of range fractional and non numeric ratings`() {
        listOf("0", "6", "-1", "3.5", "good", "", " 4", "04").forEach { score ->
            assertThrows(InvalidFeedbackScore::class.java) { validateFeedbackScore(score) }
        }
    }
}
