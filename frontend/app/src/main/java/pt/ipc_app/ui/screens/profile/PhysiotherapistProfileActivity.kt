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
import pt.ipc_app.ui.setAppContentPhysiotherapist
import pt.ipc_app.utils.viewModelInit
import java.io.IOException

/**
 * The physiotherapist profile activity.
 */
class PhysiotherapistProfileActivity : ComponentActivity() {

    private val viewModel by viewModels<PhysiotherapistProfileViewModel> {
        viewModelInit {
            val app = (application as DependenciesContainer)
            PhysiotherapistProfileViewModel(app.services.usersService, app.sessionManager)
        }
    }

    companion object {
        fun navigate(context: Context) {
            with(context) {
                val intent = Intent(this, PhysiotherapistProfileActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.changeButtonBar(ButtonBarType.PROFILE)
        viewModel.getProfile()

        setAppContentPhysiotherapist(viewModel) {
            PhysiotherapistProfileScreen(
                physiotherapist = viewModel.physiotherapistProfile.collectAsState().value,
                profilePicture = { ProfilePicture(imageRequest = viewModel.getProfilePicture(this)) },
                updateProfilePictureState = viewModel.pictureState.collectAsState().value,
                onUpdateProfilePicture = {
                    viewModel.setFileToSubmit(PhysiotherapistProfileViewModel.FileToSubmit.PICTURE)
                    openFileManager()
                },
                onSuccessUpdateProfilePicture = { Toast.makeText(this, getString(R.string.profile_picture_updated), Toast.LENGTH_SHORT).show() },
                submitCredentialDocumentState = viewModel.documentState.collectAsState().value,
                onSubmitCredentialDocument = {
                    viewModel.setFileToSubmit(PhysiotherapistProfileViewModel.FileToSubmit.CREDENTIAL)
                    openFileManager()
                },
                onSuccessSubmitCredentialDocument = {
                    Toast.makeText(this, "Document submitted!", Toast.LENGTH_SHORT).show()
                    viewModel.setDocumentSubmitted()
                },
                onLogout = {
                    LoginActivity.navigate(this)
                    finish()
                }
            )
        }
    }

    private fun openFileManager() {
        val input = when (viewModel.fileToSubmit.value) {
            PhysiotherapistProfileViewModel.FileToSubmit.PICTURE -> "image/*"
            PhysiotherapistProfileViewModel.FileToSubmit.CREDENTIAL -> "application/pdf"
            else -> "*/*"
        }

        fileChooserLauncher.launch(input)
    }

    private val fileChooserLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val file = getFileFromUri(uri) ?: throw IllegalArgumentException()

                if (viewModel.fileToSubmit.value == PhysiotherapistProfileViewModel.FileToSubmit.PICTURE)
                    viewModel.updatePicture(file)
                else if (viewModel.fileToSubmit.value == PhysiotherapistProfileViewModel.FileToSubmit.CREDENTIAL)
                    viewModel.submitCredentialDocument(file)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}