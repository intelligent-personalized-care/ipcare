package pt.ipc_app.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import pt.ipc_app.service.models.users.PhysiotherapistOutput

@Composable
fun PhysiotherapistsTable(
    physiotherapists: List<PhysiotherapistOutput>,
    onPhysiotherapistClick: (PhysiotherapistOutput) -> Unit = { }
) {
    LazyColumn {

        items(physiotherapists) {
            PhysiotherapistRow(
                physiotherapist = it,
                onPhysiotherapistClick = { onPhysiotherapistClick(it) }
            )
        }
    }
}
