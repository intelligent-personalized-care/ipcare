package pt.ipc_app.ui.components

fun parseLoadKg(text: String): Float? = text.trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() && it > 0f }
fun validLoadSelection(withLoad: Boolean?, text: String): Boolean = withLoad == false || (withLoad == true && parseLoadKg(text) != null)
