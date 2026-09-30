package pt.ipc_app.ui.components.bottomBar

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pt.ipc_app.domain.user.Role
import pt.ipc_app.domain.user.isPatient
import pt.ipc_app.ui.theme.*

@Composable
fun BottomBar(
    role: Role,
    buttonClicked: ButtonBarType,
    onHomeClick: () -> Unit = { },
    onExercisesClick: () -> Unit = { },
    onPlanCreateClick: () -> Unit = { },
    onProfileClick: () -> Unit = { }
) {
    Surface(
        elevation = 4.dp,
        color = CardBackground
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MenuButton(
                type = ButtonBarType.HOME,
                enable = buttonClicked == ButtonBarType.HOME,
                onClick = { onHomeClick() }
            )
            if (role.isPatient())
                MenuButton(
                    type = ButtonBarType.EXERCISES,
                    enable = buttonClicked == ButtonBarType.EXERCISES,
                    onClick = { onExercisesClick() }
                )
            else
                MenuButton(
                    type = ButtonBarType.PLANS,
                    enable = buttonClicked == ButtonBarType.PLANS,
                    onClick = { onPlanCreateClick() }
                )
            MenuButton(
                type = ButtonBarType.PROFILE,
                enable = buttonClicked == ButtonBarType.PROFILE,
                onClick = { onProfileClick() }
            )
        }
    }
}

@Composable
fun PatientBottomBar(
    buttonClicked: ButtonBarType = ButtonBarType.HOME,
    onHomeClick: () -> Unit = { },
    onExercisesClick: () -> Unit = { },
    onProfileClick: () -> Unit = { }
) {
    BottomBar(
        role = Role.PATIENT,
        buttonClicked = buttonClicked,
        onHomeClick = onHomeClick,
        onExercisesClick = onExercisesClick,
        onProfileClick = onProfileClick
    )
}

@Composable
fun PhysiotherapistBottomBar(
    buttonClicked: ButtonBarType = ButtonBarType.HOME,
    onHomeClick: () -> Unit = { },
    onPlanCreateClick: () -> Unit = { },
    onProfileClick: () -> Unit = { }
) {
    BottomBar(
        role = Role.PHYSIOTHERAPIST,
        buttonClicked = buttonClicked,
        onHomeClick = onHomeClick,
        onPlanCreateClick = onPlanCreateClick,
        onProfileClick = onProfileClick
    )
}

@Preview
@Composable
fun PatientBottomBarPreview() {
    PatientBottomBar()
}

@Preview
@Composable
fun PhysiotherapistBottomBarPreview() {
    PhysiotherapistBottomBar()
}
