package pt.ipc_app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.ipc_app.service.utils.Errors
import pt.ipc_app.ui.theme.*

private const val MAX_INPUT_SIZE = 100

@Composable
fun CustomTextField(
    fieldType: TextFieldType,
    textToDisplay: String,
    updateText: (String) -> Unit,
    maxLength: Int = MAX_INPUT_SIZE,
    isToTrim: Boolean = true,
    readOnly: Boolean = false,
    hide: Boolean = false,
    enabled: Boolean = true,
    iconImageVector: ImageVector? = null,
    keyboardType: KeyboardType? = null,
    error: String? = null,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val text = textToDisplay.take(maxLength)

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = if (isToTrim) text.trim() else text,
            onValueChange = updateText,
            enabled = enabled,
            singleLine = true,
            label = {
                Text(
                    text = stringResource(fieldType.labelId) + if (fieldType.required) " *" else "",
                    style = MaterialTheme.typography.body2
                )
            },
            leadingIcon = iconImageVector?.let {
                { Icon(it, contentDescription = null, tint = MediumBlue) }
            },
            trailingIcon = trailingIcon,
            readOnly = readOnly,
            shape = ButtonShape,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .semantics {
                    if (readOnly) this[SemanticsPropertyKey("ReadOnly")] = Unit
                },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colors.onSurface,
                backgroundColor = if (enabled) CardBackground else LightGrey,
                leadingIconColor = MediumBlue,
                trailingIconColor = MediumGrey,
                focusedBorderColor = if (error == null) MediumBlue else ErrorRed,
                unfocusedBorderColor = if (error == null) Grey else ErrorRed,
                disabledBorderColor = if (error == null) LightGrey else ErrorRed,
                focusedLabelColor = if (error == null) MediumBlue else ErrorRed,
                unfocusedLabelColor = if (error == null) MediumGrey else ErrorRed,
                disabledLabelColor = MediumGrey,
                disabledTextColor = MediumGrey,
                errorBorderColor = ErrorRed,
                errorLabelColor = ErrorRed
            ),
            visualTransformation = if (hide) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = if (keyboardType != null) KeyboardOptions(keyboardType = keyboardType) else KeyboardOptions.Default,
            isError = error != null
        )
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.caption,
                color = ErrorRed,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

@Preview
@Composable
fun CustomTextFieldPreview() {
    CustomTextField(
        fieldType = TextFieldType.EMAIL,
        textToDisplay = "",
        updateText = { },
        iconImageVector = Icons.Default.Face,
        error = Errors.emailAlreadyExists
    )
}
