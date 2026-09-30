package pt.ipc.api
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import pt.ipc.domain.exercises.*
class ExecutionLoadTests {
    @Test fun `load requires finite positive kilograms`() {
        validateExecutionLoad(true, 2.5f, "kg")
        validateExecutionLoad(false, null, null)
        validateExecutionLoad(null, null, null)
        listOf(null, 0f, -1f, Float.NaN, Float.POSITIVE_INFINITY).forEach {
            assertThrows(InvalidExecutionLoad::class.java) { validateExecutionLoad(true, it, "kg") }
        }
        assertThrows(InvalidExecutionLoad::class.java) { validateExecutionLoad(true, 2f, "lb") }
        assertThrows(InvalidExecutionLoad::class.java) { validateExecutionLoad(false, 2f, "kg") }
    }
}
