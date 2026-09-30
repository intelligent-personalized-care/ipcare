package pt.ipc_app

import org.junit.Assert.*
import org.junit.Test
import pt.ipc_app.ui.screens.exercises.sensor.*

class SensorExerciseGuideTests {
    @Test fun selectsThePlacementForSupportedJointsWithoutGuessingUnknownExercises() {
        assertEquals(SensorJoint.WRIST, sensorJoint("Wrist Flexion"))
        assertEquals(SensorJoint.WRIST, sensorJoint("Wrist Extension"))
        assertEquals(SensorJoint.ELBOW, sensorJoint("Elbow Flexion"))
        assertEquals(SensorJoint.KNEE, sensorJoint("Knee Extension - Seated"))
        assertEquals(SensorJoint.KNEE, sensorJoint("Supported Mini Squat"))
        assertEquals(SensorJoint.UNKNOWN, sensorJoint("New exercise"))
    }

    @Test fun countdownOnlyUsesConfirmedFirmwareHoldingProgress() {
        assertEquals(0f, sensorHoldProgress("HOLD")!!, 0f)
        assertEquals(.4f, sensorHoldProgress("HOLDING 40%")!!, .001f)
        assertEquals(1f, sensorHoldProgress("HOLDING 100%")!!, 0f)
        listOf("HOLD LOST", "RETURN TO START", "READY", "HOLDING -1%", "HOLDING 101%", "HOLDING NaN%").forEach {
            assertNull(sensorHoldProgress(it))
        }
    }

    @Test fun returnInstructionDoesNotClaimTheRepetitionHasAlreadyBeenCounted() {
        assertEquals(R.string.feedback_hold_complete_return_to_the_starting_position, sensorMovementInstruction("RETURN TO START"))
        assertEquals(R.string.feedback_position_lost_return_to_the_start_and_repeat, sensorMovementInstruction("HOLD LOST"))
        assertEquals(R.string.feedback_limit_exceeded_return_to_the_starting_position, sensorMovementInstruction("LIMIT ROLL"))
        assertEquals(R.string.feedback_sit_on_a_stable_chair_with_your_thigh_supported_and_knee_comforta, sensorStartPosition("Knee Extension - Seated"))
        assertEquals(R.string.feedback_support_your_forearm_palm_down_with_the_hand_free_beyond_the_edge, sensorStartPosition("Wrist Extension"))
        assertNull(sensorStartPosition("Unknown"))
    }
}
