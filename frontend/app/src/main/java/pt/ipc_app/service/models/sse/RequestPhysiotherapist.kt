package pt.ipc_app.service.models.sse

import java.util.UUID

data class RequestPhysiotherapist(
    val requestID: UUID,
    val name: String,
    val requestText: String?,
    val patientID: UUID
): SseEvent()
