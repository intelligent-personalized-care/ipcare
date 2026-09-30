package pt.ipc_app.ui.components

import pt.ipc_app.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadSelection(value: Boolean?, enabled: Boolean = true, onChange: (Boolean) -> Unit, loadText: String = "", onLoadText: (String) -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.ui_did_you_use_weights_for_this_set), style = MaterialTheme.typography.subtitle2)
        if (value == true) {
            OutlinedTextField(value = loadText, onValueChange = onLoadText, enabled = enabled,
                label = { Text(stringResource(R.string.ui_weight_used_kg)) }, singleLine = true,
                placeholder = { Text(stringResource(R.string.ui_e_g_2_5)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                isError = loadText.isNotBlank() && parseLoadKg(loadText) == null,
                modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.ui_enter_the_external_weight_used_for_this_set_in_kilograms), style = MaterialTheme.typography.caption)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(false to stringResource(R.string.ui_without_weights), true to stringResource(R.string.ui_with_weights)).forEach { (choice, label) ->
                OutlinedButton(onClick = { onChange(choice) }, enabled = enabled, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = if (value == choice) MaterialTheme.colors.primary.copy(alpha = .12f) else MaterialTheme.colors.surface)) {
                    Text(if (value == choice) "✓ $label" else label)
                }
            }
        }
    }
}
