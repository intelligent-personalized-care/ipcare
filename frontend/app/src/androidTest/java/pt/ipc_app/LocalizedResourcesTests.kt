package pt.ipc_app

import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocalizedResourcesTests {
    private fun resources(language: String) = InstrumentationRegistry.getInstrumentation().targetContext.let { context ->
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList(Locale.forLanguageTag(language)))
        context.createConfigurationContext(configuration).resources
    }

    @Test fun exerciseFeedbackUsesEnglishResources() {
        val res = resources("en-GB")
        assertEquals("Current angle", res.getString(R.string.ui_current_angle))
        assertEquals("3 out of 5 stars", res.getString(R.string.rating_star_value, 3))
        assertEquals("1 exercise", res.getQuantityString(R.plurals.exercise_count, 1, 1))
        assertEquals("2 exercises", res.getQuantityString(R.plurals.exercise_count, 2, 2))
    }

    @Test fun exerciseFeedbackUsesPortugueseResources() {
        val res = resources("pt-PT")
        assertEquals("Ângulo atual", res.getString(R.string.ui_current_angle))
        assertEquals("3 de 5 estrelas", res.getString(R.string.rating_star_value, 3))
        assertEquals("1 exercício", res.getQuantityString(R.plurals.exercise_count, 1, 1))
        assertEquals("2 exercícios", res.getQuantityString(R.plurals.exercise_count, 2, 2))
    }
}
