package pt.ipc_app

import org.junit.Assert.*
import org.junit.Test
import pt.ipc_app.feedback.VoiceCues
import pt.ipc_app.feedback.VoiceFeedbackGate

class VoiceFeedbackTests {
    @Test fun repeatedFramesDoNotRepeatSpeechAndCriticalEventsCanInterrupt() {
        val gate = VoiceFeedbackGate()
        assertTrue(gate.accept("Hold your position.", 0))
        assertFalse(gate.accept("Hold your position.", 50))
        assertFalse(gate.accept("Regressa", 100))
        assertTrue(gate.accept("Ligação perdida", 150, important = true))
        assertFalse(gate.accept("Ligação perdida", 200, important = true))
        assertTrue(gate.accept("Hold your position.", 6200))
    }

    @Test fun lifecycleResetAllowsFreshFeedback() {
        val gate = VoiceFeedbackGate()
        assertTrue(gate.accept("Inicia", 100))
        gate.reset()
        assertTrue(gate.accept("Inicia", 200))
        assertFalse(gate.accept("", 300, true))
    }

    @Test fun sensorCountdownUsesStableCueAndUnknownPacketsAreNotSpoken() {
        assertEquals(VoiceCues.sensor("HOLD"), VoiceCues.sensor("HOLDING 4"))
        assertEquals(VoiceCues.sensor("HOLDING 4"), VoiceCues.sensor("HOLDING 3"))
        assertEquals(VoiceCues.sensor("LIMIT PITCH HIGH"), VoiceCues.sensor("LIMIT ROLL LOW"))
        assertNull(VoiceCues.sensor("P:10|R:20"))
        assertNull(VoiceCues.camera("unrecognised"))
        assertEquals(R.string.feedback_keep_your_arms_straight, VoiceCues.camera("Ensure arms are straight"))
        assertEquals(R.string.feedback_keep_your_hands_above_your_shoulders, VoiceCues.camera("Please hold your hands above your shoulders "))
    }
}
