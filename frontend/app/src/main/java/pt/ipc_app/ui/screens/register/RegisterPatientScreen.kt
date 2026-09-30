package pt.ipc_app.ui.screens.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import pt.ipc_app.ui.components.ScreenHeader
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MonitorWeight
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
import pt.ipc_app.domain.user.Patient
import pt.ipc_app.domain.user.User
import pt.ipc_app.service.utils.ProblemJson
import pt.ipc_app.ui.components.*
import pt.ipc_app.ui.components.TextFieldType
import pt.ipc_app.ui.theme.*

const val RegisterPatientScreenTag = "RegisterPatientScreen"

private const val WEIGHT_METRIC = " kg"
private const val HEIGHT_METRIC = " cm"

@Composable
fun RegisterPatientScreen(
    progressState: ProgressState = ProgressState.IDLE,
    error: ProblemJson? = null,
    onSaveRequest: (Patient) -> Unit = { }
) {
    var userInfo: User? by remember { mutableStateOf(null) }
    var birthDate by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf(0) }
    var height by remember { mutableStateOf(0) }
    var physicalCondition by remember { mutableStateOf("") }

    val patientValidation = userInfo?.let {
        Patient.patientOrNull(
            it.name, it.email, it.password, birthDate, weight, height, physicalCondition
        )
    }

    Column(
        modifier = Modifier
            .testTag(RegisterPatientScreenTag)
            .fillMaxSize()
            .background(BackgroundWhite)
            .verticalScroll(rememberScrollState())
    ) {
        ScreenHeader(
            title = stringResource(id = R.string.register_patient_screen_title)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RegisterUser(
                        userValidation = { userInfo = it },
                        error = error
                    )

                    val dt = DatePicker(onDateSelected = { birthDate = it })
                    MyDatePicker(
                        fieldType = TextFieldType.BIRTH_DATE,
                        value = birthDate,
                        onValueChange = { birthDate = it },
                        onClick = { dt.show() }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CustomTextField(
                            fieldType = TextFieldType.WEIGHT,
                            textToDisplay = if (weight != 0) "$weight$WEIGHT_METRIC" else "",
                            updateText = { weight = it.toInteger(WEIGHT_METRIC, Patient.WEIGHT_LENGTH_MAX) },
                            iconImageVector = Icons.Default.MonitorWeight,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                        CustomTextField(
                            fieldType = TextFieldType.HEIGHT,
                            textToDisplay = if (height != 0) "$height$HEIGHT_METRIC" else "",
                            updateText = { height = it.toInteger(HEIGHT_METRIC, Patient.HEIGHT_LENGTH_MAX) },
                            iconImageVector = Icons.Default.Height,
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    CustomTextField(
                        fieldType = TextFieldType.PHYSICAL_CONDITION,
                        textToDisplay = physicalCondition,
                        updateText = { physicalCondition = it },
                        maxLength = Patient.PHYSICAL_CONDITION_LENGTH_RANGE.last,
                        isToTrim = false,
                        iconImageVector = Icons.Default.Edit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            CircularButton(
                icon = Icons.Default.Login,
                isEnabled = patientValidation != null,
                state = progressState,
                onClick = { if (patientValidation != null) onSaveRequest(patientValidation) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun String.toInteger(toSplit: String, maxLength: Int): Int {
    val value = split(toSplit)[0]
    return if (value.isEmpty() || value.toIntOrNull() == null || value.length > maxLength) 0
    else value.toInt()
}

@Preview
@Composable
fun RegisterPatientScreenPreview() {
    RegisterPatientScreen()
}
