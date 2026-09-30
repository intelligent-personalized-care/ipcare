package pt.ipc.http.models.emitter

import java.util.UUID

data class RequestPhysiotherapist(val requestID: UUID, val name: String, val requestText: String?, val patientID: UUID) : EmitterModel()
