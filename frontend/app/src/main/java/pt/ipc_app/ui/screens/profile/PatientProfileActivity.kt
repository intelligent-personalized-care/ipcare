package pt.ipc_app.ui.screens.profile

import pt.ipc_app.R

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import pt.ipc_app.DependenciesContainer
import pt.ipc_app.ui.components.ProfilePicture
import pt.ipc_app.ui.components.bottomBar.ButtonBarType
import pt.ipc_app.ui.getFileFromUri
import pt.ipc_app.ui.screens.login.LoginActivity
import pt.ipc_app.ui.setAppContentPatient
import pt.ipc_app.utils.viewModelInit
import java.io.IOException

/**
 * The patient profile activity.
 */
class PatientProfileActivity : ComponentActivity() {

    private val viewModel by viewModels<PatientProfileViewModel> {
        viewModelInit {
            val app = (application as DependenciesContainer)
            PatientProfileViewModel(app.services.usersService, app.services.sseService, app.sessionManager)
        }
    }

    companion object {
        fun navigate(context: Context) {
            with(context) {
                val intent = Intent(this, PatientProfileActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.changeButtonBar(ButtonBarType.PROFILE)
        viewModel.getProfile()

        setAppContentPatient(viewModel) {
            PatientProfileScreen(
                patient = viewModel.patientProfile.collectAsState().value,
                profilePicture = { ProfilePicture(imageRequest = viewModel.getProfilePicture(this)) },
                updateProfilePictureState = viewModel.state.collectAsState().value,
                onUpdateProfilePicture = { openGallery() },
                onSuccessUpdateProfilePicture = { Toast.makeText(this, getString(R.string.profile_picture_updated), Toast.LENGTH_SHORT).show() },
                onLogout = {
                    viewModel.unsubscribe()
                    LoginActivity.navigate(this)
                    finish()
                }
            )
        }
    }

    private fun openGallery() {
        imageChooserLauncher.launch("image/*")
    }

    private val imageChooserLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val file = getFileFromUri(uri) ?: throw IllegalArgumentException()

                viewModel.updatePicture(file)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}