package pt.ipc_app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.ui.theme.*



@Composable
fun TopBar() {
    Surface(
        elevation = 0.dp,
        color = CardBackground
    ) {
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.h6,
                        fontWeight = FontWeight.Bold,
                        color = MediumBlue
                    )
                }
            },
            backgroundColor = CardBackground,
            contentColor = MediumBlue,
            elevation = 0.dp
        )
    }
}


@Preview
@Composable
private fun TopBarPreview() {
    AppTheme {
        TopBar()
    }
}
