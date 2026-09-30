package pt.ipc_app

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pt.ipc_app.ui.components.LoadSelection
import pt.ipc_app.ui.components.parseLoadKg
import pt.ipc_app.ui.components.validLoadSelection
import pt.ipc_app.ui.theme.AppTheme

class ExecutionLoadInteractionTests {
    @get:Rule val compose = createComposeRule()

    @Test fun selectingLoadRequiresPositiveWeightAndAcceptsDecimalComma() {
        var saved: Float? = null
        compose.setContent {
            var withLoad by remember { mutableStateOf<Boolean?>(null) }
            var weight by remember { mutableStateOf("") }
            AppTheme { Column {
                LoadSelection(withLoad, onChange = { withLoad = it }, loadText = weight, onLoadText = { weight = it })
                Button(enabled = validLoadSelection(withLoad, weight), onClick = { saved = parseLoadKg(weight) }) { Text("Save test result") }
            } }
        }
        compose.onNodeWithText("Save test result").assertIsNotEnabled()
        compose.onNodeWithText("Com carga").performClick()
        compose.onNodeWithText("Save test result").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("0")
        compose.onNodeWithText("Save test result").assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextReplacement("2,5")
        compose.onNodeWithText("Save test result").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2.5f, saved) }
    }

    @Test fun switchingToNoLoadRemovesWeightInputAndAllowsSubmission() {
        compose.setContent {
            var withLoad by remember { mutableStateOf<Boolean?>(true) }
            AppTheme { Column {
                LoadSelection(withLoad, onChange = { withLoad = it }, loadText = "")
                Button(enabled = validLoadSelection(withLoad, ""), onClick = {}) { Text("Save test result") }
            } }
        }
        compose.onNode(hasSetTextAction()).assertExists()
        compose.onNodeWithText("Sem carga").performClick()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("Save test result").assertIsEnabled()
    }
}
