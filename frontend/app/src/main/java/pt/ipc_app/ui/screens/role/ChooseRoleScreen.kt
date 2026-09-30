package pt.ipc_app.ui.screens.role

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import pt.ipc_app.ui.components.ScreenHeader
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.R
import pt.ipc_app.domain.user.Role
import pt.ipc_app.ui.theme.*

const val ChooseRoleScreenTag = "ChooseRoleScreen"
const val SelectButtonTag = "SelectButton"
const val ChoosePatientButtonTag = "ChoosePatientButton"
const val ChoosePhysiotherapistButtonTag = "ChoosePhysiotherapistButton"
const val LoginButtonTag = "LoginButton"

@Composable
fun ChooseRoleScreen(
    role: Role? = null,
    onRoleChoose: (Role?) -> Unit = { },
    onRoleSelect: (Role) -> Unit = { },
    onLoginClick: () -> Unit = { }
) {
    Column(
        modifier = Modifier
            .testTag(ChooseRoleScreenTag)
            .fillMaxSize()
            .background(BackgroundWhite)
    ) {
        ScreenHeader(
            title = stringResource(id = R.string.choose_role_screen_title),
            subtitle = stringResource(id = R.string.choose_role_subtitle),
            titleAlign = TextAlign.Center
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Patient role card
            RoleOptionCard(
                selected = role == Role.PATIENT,
                onClick = { onRoleChoose(if (role != Role.PATIENT) Role.PATIENT else null) },
                modifier = Modifier.testTag(ChoosePatientButtonTag),
                imageRes = R.drawable.role_patient,
                title = stringResource(id = R.string.patient),
                description = stringResource(id = R.string.choose_role_patient_desc)
            )

            // Physiotherapist role card
            RoleOptionCard(
                selected = role == Role.PHYSIOTHERAPIST,
                onClick = { onRoleChoose(if (role != Role.PHYSIOTHERAPIST) Role.PHYSIOTHERAPIST else null) },
                modifier = Modifier.testTag(ChoosePhysiotherapistButtonTag),
                imageRes = R.drawable.role_physiotherapist,
                title = stringResource(id = R.string.physiotherapist),
                description = stringResource(id = R.string.choose_role_physiotherapist_desc)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { role?.let { onRoleSelect(it) } },
                enabled = role != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag(SelectButtonTag),
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
                Text(
                    text = stringResource(id = R.string.select),
                    style = MaterialTheme.typography.button,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.already_have_account),
                style = MaterialTheme.typography.body2,
                color = MediumBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = MutableInteractionSource(),
                        indication = null,
                        onClick = onLoginClick
                    )
                    .testTag(LoginButtonTag)
                    .padding(vertical = 12.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RoleOptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageRes: Int,
    title: String,
    description: String
) {
    val backgroundColor = if (selected) MediumBlue.copy(alpha = 0.06f) else SurfaceLight
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier.border(2.dp, MediumBlue, CardShape)
                else Modifier
            ),
        elevation = 1.dp,
        shape = CardShape
    ) {
        Row(
            modifier = Modifier.background(backgroundColor)
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) MediumBlue.copy(alpha = 0.12f) else LightGrey),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = title,
                    modifier = Modifier.size(48.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) MediumBlue else MediumGrey
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.body2,
                    color = MediumGrey
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MediumBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ChooseRoleScreenPreview() {
    ChooseRoleScreen()
}
