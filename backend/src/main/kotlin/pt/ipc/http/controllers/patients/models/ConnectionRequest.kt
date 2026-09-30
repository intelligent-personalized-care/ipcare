package pt.ipc.http.controllers.patients.models

import java.util.UUID

data class ConnectionRequest(
    val patientID: UUID,
    val text: String? = null
)
