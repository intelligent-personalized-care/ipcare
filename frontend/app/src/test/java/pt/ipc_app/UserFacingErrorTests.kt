package pt.ipc_app

import org.junit.Assert.*
import org.junit.Test
import pt.ipc_app.service.utils.*
import pt.ipc_app.ui.components.userFacing

class UserFacingErrorTests {
    @Test fun technicalDetailsAreNeverShownToThePatient() {
        val details = "Failed to connect to internal-host:5432; SQL SELECT token"
        val display = ResponseError("Request failed", details).userFacing()
        assertFalse(display.message.contains("SQL"))
        assertFalse(display.message.contains("internal-host"))
        assertTrue(display.message.contains("Tenta novamente"))
    }

    @Test fun loginFailureAndExpiredSessionHaveDifferentRecoveryInstructions() {
        val login = ProblemJson("Invalid credentials", 401).userFacing()
        val session = ProblemJson("Unauthenticated", 401).userFacing()
        assertTrue(login.message.contains("palavra-passe"))
        assertTrue(session.message.contains("Inicia sessão"))
        assertNotEquals(login, session)
    }

    @Test fun offlineAndServerFailureHaveDifferentRecoveryInstructions() {
        assertTrue(NoInternetConnection().userFacing().message.contains("Internet"))
        assertTrue(ProblemJson("Internal server error", 500).userFacing().message.contains("mais tarde"))
    }
}
