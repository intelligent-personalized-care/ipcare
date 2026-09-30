package pt.ipc_app.service.models.users

import java.util.*

data class PhysiotherapistProfile(
    val id: UUID,
    val name: String,
    val email: String,
    val rating: Rating,
    val docState: String? = null
) {
    fun documentState(): DocState =
        DocState.values().firstOrNull { it.name.equals(docState, ignoreCase = true) } ?: DocState.INVALID
}

enum class DocState {
    INVALID,
    WAITING,
    VALID
}
