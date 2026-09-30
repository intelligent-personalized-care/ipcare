package pt.ipc.domain.physiotherapist

import java.util.UUID

data class PhysiotherapistProfile(
    val id: UUID,
    val name: String,
    val email: String,
    val rating: Rating,
    val docState: String? = null
)

