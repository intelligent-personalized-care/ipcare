package pt.ipc_app.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.service.models.users.PhysiotherapistOutput
import pt.ipc_app.ui.components.ScreenHeader
import pt.ipc_app.ui.components.*
import pt.ipc_app.ui.components.TextFieldType

@Composable
fun SearchPhysiotherapistsScreen(
    physiotherapists: List<PhysiotherapistOutput>,
    requestState: ProgressState,
    onSearchRequest: (String) -> Unit = { },
    onPhysiotherapistClick: (PhysiotherapistOutput) -> Unit = { },
) {
    var typedUsername by remember { mutableStateOf(value = "") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        ScreenHeader(title = stringResource(id = R.string.search_physiotherapists))

        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        CustomTextField(
            fieldType = TextFieldType.SEARCH,
            textToDisplay = typedUsername,
            updateText = { typedUsername = it },
            iconImageVector = Icons.Default.Search
        )

        CircularButton(
            icon = Icons.Default.Search,
            isEnabled = requestState != ProgressState.WAITING,
            state = requestState,
            onClick = { onSearchRequest(typedUsername) }
        )

        Spacer(modifier = Modifier.padding(top = 10.dp))

        if (physiotherapists.isNotEmpty())
            PhysiotherapistsTable(
                physiotherapists = physiotherapists,
                onPhysiotherapistClick = onPhysiotherapistClick
            )
        else if (requestState == ProgressState.FINISHED) {
            Text(stringResource(R.string.no_physiotherapists_found), style = MaterialTheme.typography.body1)
            Text(stringResource(R.string.try_another_physiotherapist_name), style = MaterialTheme.typography.body2)
        }
        }
    }
}