package pt.ipc.services.dtos

import pt.ipc.domain.physiotherapist.Rating
import java.util.*

data class PhysiotherapistOutput(
    val id: UUID,
    val name: String,
    val email: String,
    val rating: Rating,
    val hasRated: Boolean = false
)

