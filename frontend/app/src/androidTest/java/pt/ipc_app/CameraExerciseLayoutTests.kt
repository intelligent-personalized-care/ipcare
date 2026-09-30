package pt.ipc_app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import pt.ipc_app.mlkit.posedetector.*
import pt.ipc_app.mlkit.vision.*
import pt.ipc_app.service.models.exercises.SensorProfile
import pt.ipc_app.ui.theme.AppTheme

class CameraExerciseLayoutTests {
    private fun text(id: Int, vararg args: Any) = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    @get:Rule val compose = createComposeRule()
    @Test fun failedUploadOffersLocalContinuationAndPendingRetry() {
        var continued = false
        compose.setContent {
            AppTheme {
                CameraExerciseScreen(CameraExerciseState(loading = false, pendingVideo = "pending.mp4", withLoad = false,
                    pendingUploads = 1), "Elbow Flexion", true, null, false,
                    {}, {}, {}, {}, {}, {}, {}, {}, onSaveLocally = { continued = true }) { Box(Modifier.fillMaxSize()) }
            }
        }
        compose.onNodeWithText(text(R.string.camera_retry_uploads)).assertIsEnabled()
        compose.onNodeWithText(text(R.string.camera_save_locally)).performScrollTo().performClick()
        compose.runOnIdle { org.junit.Assert.assertTrue(continued) }
    }

    @Test fun pendingResultsAreNotPresentedAsUploadedExerciseCompletion() {
        compose.setContent {
            AppTheme {
                CameraExerciseScreen(CameraExerciseState(loading = false, complete = true, pendingUploads = 2),
                    "Elbow Flexion", true, null, false, {}, {}, {}, {}, {}, {}, {}, {}) { Box(Modifier.fillMaxSize()) }
            }
        }
        compose.onNodeWithText(text(R.string.camera_recording_complete)).assertExists()
        compose.onNodeWithText(text(R.string.ui_exercise_complete_138)).assertDoesNotExist()
    }

    @Test fun displaysPrescriptionMeasuredExcursionAndHoldCountdown() {
        val profile = SensorProfile(45f, 8f, 1000, 5000, 1000, 0f, -20f, 20f, -10f, 100f)
        compose.setContent {
            AppTheme {
                CameraExerciseScreen(CameraExerciseState(loading = false, profile = profile, joint = CameraJoint.ELBOW,
                    recording = true, measurement = CameraMeasurement(angle = 47f, baseline = 5f, phase = CameraPhase.HOLD,
                        holdProgress = .4f, calibrated = true)), "Elbow Flexion", true, null, false,
                    {}, {}, {}, {}, {}, {}, {}, {}) { Box(Modifier.fillMaxSize()) }
            }
        }
        compose.onNodeWithText(text(R.string.camera_target, 45)).assertIsDisplayed()
        compose.onNodeWithText("47°").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.ui_time_remaining)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.ui_stop_set)).assertExists()
    }
    @Test fun cannotStartWithoutCalibration() {
        val profile = SensorProfile(60f, 10f, 1000, 2000, 1000, 0f, -20f, 20f, -10f, 120f)
        compose.setContent {
            AppTheme {
                CameraExerciseScreen(CameraExerciseState(loading = false, profile = profile, joint = CameraJoint.KNEE),
                    "Knee Extension", true, null, false, {}, {}, {}, {}, {}, {}, {}, {}) { Box(Modifier.fillMaxSize()) }
            }
        }
        compose.onNodeWithText(text(R.string.ui_prepare_to_start)).assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.ui_calibrate)).assertIsEnabled()
    }

    @Test fun showsAutomaticStartCountdownAndCancellation() {
        compose.setContent {
            AppTheme {
                CameraExerciseScreen(CameraExerciseState(loading = false, autoStartPending = true, startCountdown = 3,
                    measurement = CameraMeasurement(angle = 0f, phase = CameraPhase.READY, calibrated = true)),
                    "Elbow Flexion", true, null, false, {}, {}, {}, {}, {}, {}, {}, {}) { Box(Modifier.fillMaxSize()) }
            }
        }
        compose.onNodeWithText(text(R.string.ui_starting_in)).assertIsDisplayed()
        compose.onNodeWithText("3").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.ui_cancel_start)).assertIsEnabled()
    }
}
