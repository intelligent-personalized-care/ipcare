package pt.ipc_app

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test
import pt.ipc_app.service.models.exercises.SensorSession
import pt.ipc_app.service.models.exercises.SensorSample
import pt.ipc_app.service.models.exercises.ExerciseVideoFeedback
import pt.ipc_app.service.models.plans.LibraryPlan
import java.util.UUID

class ExecutionLoadTests {
    @Test fun weightInputAcceptsDecimalsAndRejectsInvalidValues() {
        assertEquals(2.5f, pt.ipc_app.ui.components.parseLoadKg("2,5"))
        listOf("", "0", "-1", "NaN", "Infinity", "1e99", "abc").forEach {
            assertFalse(pt.ipc_app.ui.components.validLoadSelection(true, it))
        }
        assertTrue(pt.ipc_app.ui.components.validLoadSelection(false, ""))
        assertFalse(pt.ipc_app.ui.components.validLoadSelection(null, ""))
    }

    @Test fun loadAnswerSurvivesPendingSessionSerialization() {
        val json = Gson()
        listOf(true, false, null).forEach { load ->
            val result = SensorSession(UUID.randomUUID(), 1, 10, 1000, listOf(SensorSample(0, 0f, 0f, 0f)), withLoad = load, loadValue = if (load == true) 2.5f else null, loadUnit = if (load == true) "kg" else null)
            assertEquals(result, json.fromJson(json.toJson(result), SensorSession::class.java))
        }
        val feedback = json.fromJson("""{"withLoad":true,"loadValue":2.5,"loadUnit":"kg"}""", ExerciseVideoFeedback::class.java)
        assertEquals(2.5f, feedback.loadValue)
        assertEquals("kg", feedback.loadUnit)
        assertNull(json.fromJson("{}", ExerciseVideoFeedback::class.java).withLoad)
        assertEquals(false, json.fromJson("{\"withLoad\":false}", ExerciseVideoFeedback::class.java).withLoad)
    }
    @Test fun templateDoesNotNeedAnAssignmentDateAndPreservesRestDays() {
        val plan = Gson().fromJson("{\"id\":1,\"title\":\"Rehabilitation\",\"dailyLists\":[null,{\"id\":2,\"exercises\":[]}]}", LibraryPlan::class.java)
        assertEquals(2, plan.dailyLists.size)
        assertNull(plan.dailyLists.first())
        assertTrue(plan.dailyLists[1]!!.exercises.isEmpty())
    }
}
