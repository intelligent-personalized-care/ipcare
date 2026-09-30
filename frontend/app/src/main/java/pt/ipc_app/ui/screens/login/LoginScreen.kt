package pt.ipc_app.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.domain.user.User
import pt.ipc_app.ui.components.TextFieldType
import pt.ipc_app.ui.components.*
import pt.ipc_app.ui.components.ScreenHeader
import pt.ipc_app.ui.theme.*

const val LoginScreenTag = "LoginScreen"

@Composable
fun LoginScreen(
    progressState: ProgressState = ProgressState.IDLE,
    onSaveRequest: (email: String, password: String) -> Unit = { _,_ -> }
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .testTag(LoginScreenTag)
            .fillMaxSize()
            .background(BackgroundWhite)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(
                title = stringResource(id = R.string.app_name),
                subtitle = stringResource(id = R.string.login_tagline),
                titleAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 1.dp,
                shape = CardShape,
                backgroundColor = SurfaceLight
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.login_screen_title),
                        style = MaterialTheme.typography.h5,
                        fontWeight = FontWeight.Bold,
                        color = MediumBlue,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(id = R.string.login_welcome),
                        style = MaterialTheme.typography.body2,
                        color = MediumGrey,
                        textAlign = TextAlign.Center
                    )

                    CustomTextField(
                        fieldType = TextFieldType.EMAIL,
                        textToDisplay = email,
                        updateText = { email = it },
                        maxLength = User.EMAIL_LENGTH_RANGE.last,
                        iconImageVector = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CustomTextField(
                        fieldType = TextFieldType.PASSWORD,
                        textToDisplay = password,
                        updateText = { password = it },
                        iconImageVector = Icons.Default.Password,
                        hide = !passwordVisible,
                        keyboardType = KeyboardType.Password,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = MediumGrey
                                )
                            }
                        }
                    )

                    Button(
                        onClick = { onSaveRequest(email, password) },
                        enabled = User.validateEmail(email) && User.validatePassword(password) && progressState != ProgressState.WAITING,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = ButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MediumBlue,
                            contentColor = White,
                            disabledBackgroundColor = LightGrey,
                            disabledContentColor = MediumGrey
                        ),
                        elevation = ButtonDefaults.elevation(
                            defaultElevation = 2.dp,
                            pressedElevation = 4.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        if (progressState == ProgressState.WAITING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = stringResource(R.string.label_sign_in),
                                modifier = Modifier.size(20.dp),
                                tint = White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(id = R.string.login_button),
                                style = MaterialTheme.typography.button,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}
