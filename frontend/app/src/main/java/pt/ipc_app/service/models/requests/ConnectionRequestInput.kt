package pt.ipc_app.service.models.requests

import java.util.*

data class ConnectionRequestInput(
    val patientID: UUID,
    val text: String? = null
)