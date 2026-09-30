package pt.ipc_app

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import pt.ipc_app.domain.user.Role
import pt.ipc_app.service.models.exercises.ExerciseVideoFeedback

class ApiContractTests {
    private val gson = Gson()
    @Test fun serializesCanonicalRoleNames() {
        assertEquals(Role.PATIENT, gson.fromJson("\"PATIENT\"", Role::class.java))
        assertEquals("\"PATIENT\"", gson.toJson(Role.PATIENT))
        assertEquals(Role.PHYSIOTHERAPIST, gson.fromJson("\"PHYSIOTHERAPIST\"", Role::class.java))
        assertEquals("\"PHYSIOTHERAPIST\"", gson.toJson(Role.PHYSIOTHERAPIST))
    }
    @Test fun emptyPhysiotherapistSearchIsAValidResponse() {
        val result = gson.fromJson("""{"physiotherapists":[]}""", pt.ipc_app.service.models.users.ListPhysiotherapistsOutput::class.java)
        assertNotNull(result.physiotherapists)
        assertTrue(result.physiotherapists.isEmpty())
    }
    @Test fun readsFeedbackFromCurrentBackend() {
        val feedback = gson.fromJson("""{"patientFeedBack":"Tired","physiotherapistFeedBack":"Good","executionMode":"sensor","physiotherapistFeedbackScore":"4"}""", ExerciseVideoFeedback::class.java)
        assertEquals("Good", feedback.physiotherapistFeedBack)
        assertEquals("Tired", feedback.patientFeedBack)
        assertEquals("sensor", feedback.executionMode)
        assertEquals("4", feedback.physiotherapistFeedbackScore)
    }
}
