package pt.ipc_app.ui.screens.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.R
import pt.ipc_app.ui.screens.details.PhysiotherapistDetailsActivity
import pt.ipc_app.ui.setAppContentPatient
import pt.ipc_app.utils.viewModelInit

/**
 * The search physiotherapists activity.
 */
class SearchPhysiotherapistsActivity : ComponentActivity() {

    private val viewModel by viewModels<SearchPhysiotherapistsViewModel> {
        viewModelInit {
            val app = (application as DependenciesContainer)
            SearchPhysiotherapistsViewModel(app.services.usersService, app.sessionManager)
        }
    }

    companion object {
        fun navigate(context: Context) {
            with(context) {
                val intent = Intent(this, SearchPhysiotherapistsActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setAppContentPatient(viewModel) {

            SearchPhysiotherapistsScreen(
                physiotherapists = viewModel.physiotherapists.collectAsState().value,
                requestState = viewModel.state.collectAsState().value,
                onSearchRequest = { viewModel.searchPhysiotherapists(it) },
                onPhysiotherapistClick = { PhysiotherapistDetailsActivity.navigate(this, it) }
            )
        }
    }
}