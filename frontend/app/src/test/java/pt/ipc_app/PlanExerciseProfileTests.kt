package pt.ipc_app

import org.junit.Assert.*
import org.junit.Test
import pt.ipc_app.service.models.exercises.SensorProfile
import pt.ipc_app.ui.screens.plan.planExerciseProfile

class PlanExerciseProfileTests {
    private val base = SensorProfile(30f, 5f, 1000, 5000, 1000, 0f, -20f, 20f, -10f, 60f)

    @Test fun defaultsDoNotFreezeCatalogueValues() {
        assertNull(planExerciseProfile(base, false, "45", "10", "2"))
        assertNull(planExerciseProfile(null, false, "", "", ""))
    }
    @Test fun customizationChangesOnlyRequestedFieldsAndSupportsDecimalComma() {
        val changed = planExerciseProfile(base, true, "42,5", "8", "2,5")!!
        assertEquals(42.5f, changed.raiseThreshold, 0f)
        assertEquals(2500, changed.holdTimeMs)
        assertEquals(base.minRoll, changed.minRoll, 0f)
        assertEquals(30f, base.raiseThreshold, 0f)
    }
    @Test fun invalidRangesAndNonfiniteInputsAreRejected() {
        listOf(Triple("61", "5", "2"), Triple("5", "5", "2"), Triple("NaN", "5", "2"), Triple("30", "5", "61")).forEach { (target, returned, hold) ->
            assertThrows(IllegalArgumentException::class.java) { planExerciseProfile(base, true, target, returned, hold) }
        }
    }
}
