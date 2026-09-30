package pt.ipc_app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.TextButton
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.R

@Composable
fun ErrorAlert(
    title: String,
    message: String? = null,
    buttonText: String = stringResource(R.string.ok),
    onDismiss: () -> Unit = { }
) {
    var showDialog by remember(title, message) { mutableStateOf(true) }

    if (showDialog)
        AlertDialog(
            onDismissRequest = { showDialog = false; onDismiss() },
            buttons = {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp)
                ) {
                    TextButton(
                        onClick = {
                            showDialog = false
                            onDismiss()
                        }
                    ) {
                        Text(text = buttonText)
                    }
                }
            },
            title = { Text(text = title) },
            text = message?.let { { Text(text = it) } }
        )
}

@Preview(showBackground = true)
@Composable
private fun ErrorAlertPreview() {
    ErrorAlert(
        title = stringResource(R.string.error_accessing_server),
        message = "Could not …",
        buttonText = stringResource(R.string.ok),
        onDismiss = { }
    )
}
