package pt.ipc_app.ui.components

import pt.ipc_app.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import pt.ipc_app.ui.theme.MediumBlue

@Composable
fun ButtonToUpdatePicture(
    updateProfilePictureState: ProgressState = ProgressState.IDLE,
    onUpdateProfilePicture: () -> Unit = { },
    onSuccessUpdateProfilePicture: () -> Unit = { }
) {
    Icon(
        imageVector = Icons.Default.Edit,
        contentDescription = stringResource(R.string.ui_change_profile_picture),
        tint = MediumBlue,
        modifier = Modifier.clickable(
            interactionSource = MutableInteractionSource(),
            indication = null,
            enabled = updateProfilePictureState != ProgressState.WAITING,
            onClick = onUpdateProfilePicture
        )
    )

    if (updateProfilePictureState == ProgressState.WAITING) {
        CircularProgressIndicator()
    } else {
        if (updateProfilePictureState == ProgressState.FINISHED)
            onSuccessUpdateProfilePicture()
    }
}