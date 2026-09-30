package pt.ipc.domain.patient

import java.util.UUID

data class PatientInformation(
    val id: UUID,
    val name: String,
    val email: String
)
