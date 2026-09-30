package pt.ipc.domain.physiotherapist

import java.util.UUID

data class PhysiotherapistAvailable(
    val id: UUID,
    val name: String,
    val email: String,
    val rating: Rating? = null,
    val requested: Boolean
)

