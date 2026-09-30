package pt.ipc_app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import pt.ipc_app.ui.screens.exercises.selection.ChooseCameraOrSensorScreen
import pt.ipc_app.ui.screens.details.PhysiotherapistDetailsScreen
import pt.ipc_app.ui.components.VideoPlayer
import pt.ipc_app.ui.theme.AppTheme
import pt.ipc_app.service.models.users.PhysiotherapistOutput
import pt.ipc_app.service.models.users.Rating
import java.util.UUID

class ExerciseUiAvailabilityTests {
    @get:Rule val compose = createComposeRule()
    private fun text(id: Int) = InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    @Test fun unsupportedCameraCannotBeSelected() {
        var calls = 0
        compose.setContent { AppTheme {
            ChooseCameraOrSensorScreen({ calls++ }, { calls++ }, cameraEnabled = false, sensorsEnabled = true)
        } }
        compose.onNodeWithText(text(R.string.use_camera)).assertIsNotEnabled().performClick()
        compose.runOnIdle { assertEquals(0, calls) }
        compose.onNodeWithText(text(R.string.use_sensors)).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun unsupportedSensorsCannotBeSelected() {
        compose.setContent { AppTheme {
            ChooseCameraOrSensorScreen({}, {}, cameraEnabled = true, sensorsEnabled = false)
        } }
        compose.onNodeWithText(text(R.string.use_camera)).assertIsEnabled()
        compose.onNodeWithText(text(R.string.use_sensors)).assertIsNotEnabled()
    }

    @Test fun existingRatingHidesSubmissionControls() {
        compose.setContent { AppTheme {
            PhysiotherapistDetailsScreen(PhysiotherapistOutput(UUID.randomUUID(), "Test", "test@example.test",
                Rating(4f, 1), isMyPhysiotherapist = true, hasRated = true))
        } }
        compose.onNodeWithText("Avaliar fisioterapeuta").assertDoesNotExist()
        compose.onNodeWithText("Enviar avaliação").assertDoesNotExist()
    }

    @Test fun unratedPatientCanRate() {
        compose.setContent { AppTheme {
            PhysiotherapistDetailsScreen(PhysiotherapistOutput(UUID.randomUUID(), "Test", "test@example.test",
                Rating(4f, 1), isMyPhysiotherapist = true, hasRated = false))
        } }
        compose.onNodeWithText("Avaliar fisioterapeuta").assertExists()
    }

    @Test fun videoCanEnterAndLeaveFullscreen() {
        compose.setContent { AppTheme { VideoPlayer("", playing = false, muted = true) } }
        compose.onNodeWithContentDescription(text(R.string.video_fullscreen)).performClick()
        compose.onNodeWithContentDescription(text(R.string.video_exit_fullscreen)).assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription(text(R.string.video_fullscreen)).assertIsDisplayed()
    }
}
