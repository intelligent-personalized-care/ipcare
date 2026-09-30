package pt.ipc_app

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pt.ipc_app.ble.BleUiState
import pt.ipc_app.service.models.exercises.SensorProfile
import pt.ipc_app.ui.components.ExecutionRatingStars
import pt.ipc_app.ui.screens.exercises.sensor.LiveExerciseCard
import pt.ipc_app.ui.theme.AppTheme

class ExecutionFeedbackUiTests {
    private fun text(id: Int, vararg args: Any) = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    @get:Rule val compose = createComposeRule()

    @Test fun physiotherapistCanChooseAndChangeStarRating() {
        var chosen: Int? = null
        compose.setContent {
            var rating by remember { mutableStateOf<Int?>(null) }
            AppTheme { ExecutionRatingStars(rating, onRating = { rating = it; chosen = it }) }
        }
        compose.onNodeWithContentDescription(text(R.string.rating_star_value, 3)).performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(3, chosen) }
        compose.onNodeWithContentDescription(text(R.string.rating_star_value, 5)).performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(5, chosen) }
    }

    @Test fun patientRatingIsReadOnly() {
        compose.setContent { AppTheme { ExecutionRatingStars(4) } }
        compose.onNodeWithText(" 4/5").assertIsDisplayed()
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test fun sensorPanelShowsAngleCountdownAndRepetitionsWithoutTechnicalClutter() {
        val profile = SensorProfile(60f, 10f, 1000, 5000, 1000, 0f, -10f, 100f, -25f, 25f, false)
        compose.setContent { AppTheme {
            LiveExerciseCard(BleUiState(profile = profile, connected = true, sessionRunning = true,
                sensorAngle = 62f, sensorPitch = 62f, sensorRoll = 4f, sensorReps = 3,
                sensorStatus = "HOLDING 40%", rollHistory = listOf(0f, 20f, 40f, 62f)),
                10, 3, 60f, 10f, {})
        } }
        compose.onNodeWithText("62°").assertIsDisplayed()
        compose.onNodeWithText("≈ 3 s").assertIsDisplayed()
        compose.onNodeWithText(text(R.string.repetition_progress, 3, 10)).assertIsDisplayed()
        compose.onNodeWithText(text(R.string.sensor_target_bound, "60")).assertIsDisplayed()
        val directory = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        File(directory, "sensor-live-panel.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("Pitch:", substring = true).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.ui_sensor_details)).performClick()
        compose.onNodeWithText("Pitch: 62° · Roll: 4°").assertExists()
    }
}
