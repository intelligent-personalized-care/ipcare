package pt.ipc_app

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/** Checks translation coverage and formatting without requiring a running Android device. */
class StringResourcesTests {
    private fun entries(folder: String): Map<String, Element> {
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(File("src/main/res/$folder/strings.xml")).documentElement
        val result = linkedMapOf<String, Element>()
        for (index in 0 until root.childNodes.length) {
            val node = root.childNodes.item(index) as? Element ?: continue
            val name = node.getAttribute("name")
            assertNull("Duplicate resource $folder/$name", result.put(name, node))
        }
        return result
    }

    @Test fun allTranslatableResourcesHavePortugueseTranslations() {
        val english = entries("values")
        val portuguese = entries("values-pt")
        english.filterValues { it.getAttribute("translatable") != "false" }.forEach { (name, value) ->
            assertTrue("Missing Portuguese translation: $name", portuguese.containsKey(name))
            assertEquals("Resource type differs: $name", value.tagName, portuguese[name]!!.tagName)
        }
    }

    @Test fun translatedFormatArgumentsMatchAndCanBeFormatted() {
        val pattern = Regex("%(?:(\\d+)\\$)?[-#+ 0,(]*\\d*(?:\\.\\d+)?([dsfg])")
        val english = entries("values")
        val portuguese = entries("values-pt")
        for ((name, node) in english.filterValues { it.tagName == "string" && it.getAttribute("translatable") != "false" }) {
            val en = node.textContent
            val pt = portuguese.getValue(name).textContent
            fun signature(value: String) = pattern.findAll(value).mapIndexed { index, match ->
                (match.groupValues[1].toIntOrNull() ?: index + 1) to match.groupValues[2]
            }.toList()
            assertEquals("Placeholder mismatch: $name", signature(en).sortedBy { it.first }, signature(pt).sortedBy { it.first })
            val placeholders = signature(en)
            if (placeholders.isEmpty()) continue
            val args = Array<Any>(placeholders.maxOf { it.first }) { "sample" }
            placeholders.forEach { (index, type) -> args[index - 1] = when (type) { "d" -> 2; "f", "g" -> 2.5; else -> "sample" } }
            assertTrue(String.format(Locale.ENGLISH, en, *args).isNotEmpty())
            assertTrue(String.format(Locale("pt", "PT"), pt, *args).isNotEmpty())
        }
    }

    @Test fun resourcesHaveNoBrokenEncodingAndCountsHaveSingularAndPlural() {
        for (folder in listOf("values", "values-pt")) {
            for ((name, node) in entries(folder)) {
                assertFalse("Broken encoding: $folder/$name", Regex("Ã[£©§µ]|Â[°·]|â€|�").containsMatchIn(node.textContent))
                if (node.tagName == "plurals") {
                    val items = node.getElementsByTagName("item")
                    val quantities = (0 until items.length).map { (items.item(it) as Element).getAttribute("quantity") }
                    assertTrue("Missing singular: $name", "one" in quantities)
                    assertTrue("Missing plural: $name", "other" in quantities)
                }
            }
        }
    }
}
