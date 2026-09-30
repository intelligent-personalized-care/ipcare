package pt.ipc_app

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import pt.ipc_app.ble.BleUiState
import pt.ipc_app.service.models.exercises.SensorProfile
import pt.ipc_app.ui.screens.exercises.sensor.SensorPlacementCard
import pt.ipc_app.ui.screens.exercises.sensor.SensorTargets
import pt.ipc_app.ui.theme.AppTheme
import java.io.File

class SensorPreparationLayoutTests {
    @get:Rule val compose = createComposeRule()

    private fun placement(title: String, proximal: String, distal: String, screenshot: String) {
        compose.setContent {
            AppTheme {
                Surface {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                        SensorPlacementCard(title, "")
                    }
                }
            }
        }
        compose.onNodeWithText(proximal, substring = true).assertIsDisplayed()
        compose.onNodeWithText(distal, substring = true).assertIsDisplayed()
        capture(screenshot)
    }

    @Test fun wrist() = placement("Wrist Flexion", "Antebraço", "Dorso da mão", "wrist")
    @Test fun elbow() = placement("Elbow Flexion", "Braço, acima do cotovelo", "Antebraço, abaixo do cotovelo", "elbow")
    @Test fun knee() = placement("Knee Extension - Seated", "Coxa, acima do joelho", "Perna, abaixo do joelho", "knee")

    @Test fun targetsAndFirmwareHoldTime() {
        val profile = SensorProfile(60f, 10f, 1000, 5000, 1000, 0f, -25f, 25f, -10f, 100f)
        compose.setContent {
            AppTheme {
                Surface {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
                        SensorTargets(profile, BleUiState(profile = profile, sessionRunning = true, sensorRoll = 62f, sensorStatus = "HOLDING 40%"))
                    }
                }
            }
        }
        compose.onNodeWithText("≥ 60°").assertIsDisplayed()
        compose.onNodeWithText("5 s").assertIsDisplayed()
        compose.onNodeWithText("Faltam aproximadamente 3 s").assertIsDisplayed()
        capture("targets")
    }

    private fun capture(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "sensor-$name.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
