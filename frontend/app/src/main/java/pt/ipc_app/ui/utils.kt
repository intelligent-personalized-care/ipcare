package pt.ipc_app.ui

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.runtime.LaunchedEffect
import pt.ipc_app.ui.components.userFacing
import pt.ipc_app.ui.screens.home.PatientHomeActivity
import pt.ipc_app.ui.screens.home.PhysiotherapistHomeActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import pt.ipc_app.R
import pt.ipc_app.TAG
import pt.ipc_app.service.utils.ProblemJson
import pt.ipc_app.ui.components.ErrorAlert
import pt.ipc_app.ui.screens.AppPatientScreen
import pt.ipc_app.ui.screens.AppPhysiotherapistScreen
import pt.ipc_app.ui.screens.AppViewModel
import pt.ipc_app.ui.screens.login.LoginActivity
import pt.ipc_app.ui.theme.AppTheme
import java.io.File
import java.io.FileOutputStream

fun Context.openSendEmail(email: String) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        }
        startActivity(intent)
    }
    catch (e: ActivityNotFoundException) {
        Log.e(TAG, "Failed to send email", e)
        Toast
            .makeText(
                this,
                R.string.activity_info_no_suitable_app,
                Toast.LENGTH_LONG
            )
            .show()
    }
}

fun Context.getFileFromUri(imageUri: Uri): File? {
    val documentFile = DocumentFile.fromSingleUri(this, imageUri)
    return documentFile?.let { file ->
        val inputStream = contentResolver.openInputStream(file.uri)
        val outputFile = file.name?.let { File(this.cacheDir, it) }
        val outputStream = FileOutputStream(outputFile)
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        outputFile
    }
}

fun Context.createNotification(
    channelId: String,
    name: String
) {
    val importance = NotificationManager.IMPORTANCE_DEFAULT
    val channel = NotificationChannel(channelId, name, importance)
    // Register the channel with the system
    val notificationManager: NotificationManager =
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.createNotificationChannel(channel)
}

private fun ComponentActivity.setAppContent(
    viewModel: AppViewModel,
    content: @Composable () -> Unit
) {
    setContent {
        content()
        val error = viewModel.error.collectAsState().value
        val isHome = this is PatientHomeActivity || this is PhysiotherapistHomeActivity
        if (error != null) {
            val expired = error is ProblemJson && error.unauthenticatedResponse()
            if (isHome) {
                LaunchedEffect(error) {
                    viewModel.dismissError()
                    if (expired) { LoginActivity.navigate(this@setAppContent); finish() }
                }
            } else {
                val display = error.userFacing()
                AppTheme {
                    ErrorAlert(title = display.title, message = display.message, onDismiss = {
                        viewModel.dismissError()
                        if (expired) { LoginActivity.navigate(this@setAppContent); finish() }
                    })
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
fun ComponentActivity.setAppContentInitial(
    viewModel: AppViewModel,
    content: @Composable () -> Unit
) {
    setAppContent(viewModel) {
        AppTheme {
            Scaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background),
                content = { content() }
            )
        }
    }
}


fun ComponentActivity.setAppContentPatient(
    viewModel: AppViewModel,
    content: @Composable () -> Unit
) {
    setAppContent(viewModel) {
        AppPatientScreen(
            buttonBarClicked = viewModel.buttonBarClicked.collectAsState().value,
            onNavigated = ::finish,
            content = content
        )
    }
}

fun ComponentActivity.setAppContentPhysiotherapist(
    viewModel: AppViewModel,
    content: @Composable () -> Unit
) {
    setAppContent(viewModel) {
        AppPhysiotherapistScreen(
            buttonBarClicked = viewModel.buttonBarClicked.collectAsState().value,
            onNavigated = ::finish,
            content = content
        )
    }
}
