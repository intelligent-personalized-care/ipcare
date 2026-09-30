package pt.ipc_app.feedback

import pt.ipc_app.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun VoiceFeedbackControl(voice: ExerciseVoiceFeedback) {
    val enabled by voice.enabled.collectAsState()
    val unavailable by voice.unavailable.collectAsState()
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.ui_voice_feedback))
            Spacer(Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = voice::setEnabled)
        }
        if (unavailable) Text(stringResource(R.string.ui_voice_unavailable_enable_a_voice_for_the_app_language_in_your_pho))
    }
}
