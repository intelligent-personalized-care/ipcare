package pt.ipc_app.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import pt.ipc_app.ui.components.bottomBar.ButtonBarType
import pt.ipc_app.ui.components.bottomBar.PatientBottomBar
import pt.ipc_app.ui.components.bottomBar.PhysiotherapistBottomBar
import pt.ipc_app.ui.screens.exercises.list.ExercisesListActivity
import pt.ipc_app.ui.screens.home.PatientHomeActivity
import pt.ipc_app.ui.screens.home.PhysiotherapistHomeActivity
import pt.ipc_app.ui.screens.plan.CreatePlanActivity
import pt.ipc_app.ui.screens.profile.PatientProfileActivity
import pt.ipc_app.ui.screens.profile.PhysiotherapistProfileActivity
import pt.ipc_app.ui.theme.AppTheme
import pt.ipc_app.ui.components.TopBar

/**
 * A screen that displays the app of patient.
 *
 * @param content the content to be displayed
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AppPatientScreen(
    buttonBarClicked: ButtonBarType = ButtonBarType.HOME,
    onNavigated: () -> Unit = { },
    content: @Composable () -> Unit
) {
    AppTheme {
        val ctx = LocalContext.current
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            topBar = {
                TopBar()
            },
            bottomBar = {
                PatientBottomBar(
                    buttonClicked = buttonBarClicked,
                    onHomeClick = {
                        PatientHomeActivity.navigate(ctx)
                        onNavigated()
                    },
                    onExercisesClick = {
                        ExercisesListActivity.navigate(ctx)
                        onNavigated()
                    },
                    onProfileClick = {
                        PatientProfileActivity.navigate(ctx)
                        onNavigated()
                    }
                )
            },
            content = { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    content()
                }
            }
        )
    }
}

/**
 * A screen that displays the app of physiotherapist.
 *
 * @param content the content to be displayed
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun AppPhysiotherapistScreen(
    buttonBarClicked: ButtonBarType = ButtonBarType.HOME,
    onNavigated: () -> Unit = { },
    content: @Composable () -> Unit
) {
    AppTheme {
        val ctx = LocalContext.current
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            topBar = {
                TopBar()
            },
            bottomBar = {
                PhysiotherapistBottomBar(
                    buttonClicked = buttonBarClicked,
                    onHomeClick = {
                        PhysiotherapistHomeActivity.navigate(ctx)
                        onNavigated()
                    },
                    onPlanCreateClick = {
                        pt.ipc_app.ui.screens.plan.PlanLibraryActivity.navigate(ctx)
                        onNavigated()
                    },
                    onProfileClick = {
                        PhysiotherapistProfileActivity.navigate(ctx)
                        onNavigated()
                    }
                )
            },
            content = { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    content()
                }
            }
        )
    }
}
