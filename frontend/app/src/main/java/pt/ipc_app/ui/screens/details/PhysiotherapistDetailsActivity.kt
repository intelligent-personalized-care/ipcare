package pt.ipc_app.ui.screens.details

import androidx.compose.runtime.collectAsState
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.service.models.users.PhysiotherapistOutput
import pt.ipc_app.ui.components.ProfilePicture
import pt.ipc_app.ui.openSendEmail
import pt.ipc_app.ui.setAppContentPatient
import pt.ipc_app.utils.viewModelInit

/**
 * The physiotherapist details activity.
 */
class PhysiotherapistDetailsActivity : ComponentActivity() {

    private val viewModel by viewModels<PhysiotherapistDetailsViewModel> {
        viewModelInit {
            val app = (application as DependenciesContainer)
            PhysiotherapistDetailsViewModel(app.services.usersService, app.sessionManager)
        }
    }

    companion object {
        const val PHYSIOTHERAPIST = "PHYSIOTHERAPIST"
        fun navigate(context: Context, physiotherapist: PhysiotherapistOutput) {
            with(context) {
                val intent = Intent(this, PhysiotherapistDetailsActivity::class.java)
                intent.putExtra(PHYSIOTHERAPIST, physiotherapist)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (physiotherapist.isMyPhysiotherapist) viewModel.refreshPhysiotherapist()
        setAppContentPatient(viewModel) {
            PhysiotherapistDetailsScreen(
                physiotherapist = viewModel.currentPhysiotherapist.collectAsState().value
                    ?: physiotherapist.copy(hasRated = true),
                profilePicture = { ProfilePicture(imageRequest = viewModel.getProfilePicture(this, physiotherapist.id)) },
                onSendEmailRequest = { openSendEmail(physiotherapist.email) },
                onRequestedConnection = { viewModel.connectWithPhysiotherapist(physiotherapist.id, it, onSuccess = ::finish) },
                onRemovePatient = { viewModel.disconnectPhysiotherapist(onSuccess = ::finish) },
                onRatedPhysiotherapist = { viewModel.ratePhysiotherapist(physiotherapist.id, it, onSuccess = ::finish) }
            )
        }
    }

    @Suppress("deprecation")
    private val physiotherapist: PhysiotherapistOutput by lazy {
        val physiotherapist = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            intent.getParcelableExtra(PHYSIOTHERAPIST, PhysiotherapistOutput::class.java)
        else
            intent.getParcelableExtra(PHYSIOTHERAPIST)
        checkNotNull(physiotherapist)
    }
}
