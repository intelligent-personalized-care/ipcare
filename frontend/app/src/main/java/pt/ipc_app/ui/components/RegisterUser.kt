package pt.ipc_app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.domain.user.User
import pt.ipc_app.domain.user.User.Companion.userOrNull
import pt.ipc_app.service.utils.ProblemJson

@Composable
fun RegisterUser(
    userValidation: (User?) -> Unit,
    error: ProblemJson? = null
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    userValidation(userOrNull(name, email, password))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CustomTextField(
            fieldType = TextFieldType.NAME,
            textToDisplay = name,
            updateText = { name = it },
            maxLength = User.NAME_LENGTH_RANGE.last,
            isToTrim = false,
            iconImageVector = Icons.Default.Face,
            modifier = Modifier.fillMaxWidth()
        )
        CustomTextField(
            fieldType = TextFieldType.EMAIL,
            textToDisplay = email,
            updateText = { email = it },
            maxLength = User.EMAIL_LENGTH_RANGE.last,
            iconImageVector = Icons.Default.Email,
            keyboardType = KeyboardType.Email,
            error = error?.let { TextFieldType.EMAIL.errorToShow(it) },
            modifier = Modifier.fillMaxWidth()
        )
        CustomTextField(
            fieldType = TextFieldType.PASSWORD,
            textToDisplay = password,
            updateText = { password = it },
            iconImageVector = Icons.Default.Password,
            hide = true,
            keyboardType = KeyboardType.Password,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview
@Composable
private fun RegisterUserPreview() {
    RegisterUser(
        userValidation = {}
    )
}
