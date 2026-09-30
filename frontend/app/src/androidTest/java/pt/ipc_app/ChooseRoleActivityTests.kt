package pt.ipc_app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pt.ipc_app.ui.screens.login.LoginScreenTag
import pt.ipc_app.ui.screens.register.RegisterPatientScreenTag
import pt.ipc_app.ui.screens.register.RegisterPhysiotherapistScreenTag
import pt.ipc_app.ui.screens.role.*
import java.util.*

@RunWith(AndroidJUnit4::class)
class ChooseRoleActivityTests {

    @get:Rule
    val testRule = createAndroidComposeRule<ChooseRoleActivity>()

    private val app by lazy {
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .applicationContext as IPCApplication
    }

    private val user = if (app.sessionManager.isLoggedIn()) app.sessionManager.userLoggedIn else null

    @Before
    fun clearSession() {
        app.sessionManager.clearSession()
    }

    @After
    fun resetSession() {
        user?.let {
            app.sessionManager.setSession(it.id, it.name, it.accessToken, it.refreshToken, it.role)
        }
    }

    @Test
    fun choosing_patient_role_navigates_to_register_patient() {

        testRule.onNodeWithTag(ChoosePatientButtonTag).performClick()
        testRule.onNodeWithTag(SelectButtonTag).performClick()
        testRule.waitForIdle()

        testRule.onNodeWithTag(RegisterPatientScreenTag).assertExists()
    }

    @Test
    fun choosing_physiotherapist_role_navigates_to_register_physiotherapist() {

        testRule.onNodeWithTag(ChoosePhysiotherapistButtonTag).performClick()
        testRule.onNodeWithTag(SelectButtonTag).performClick()
        testRule.waitForIdle()

        testRule.onNodeWithTag(RegisterPhysiotherapistScreenTag).assertExists()
    }

    @Test
    fun pressing_login_navigates_to_login() {

        testRule.onNodeWithTag(LoginButtonTag).performClick()
        testRule.waitForIdle()

        testRule.onNodeWithTag(LoginScreenTag).assertExists()
    }

}