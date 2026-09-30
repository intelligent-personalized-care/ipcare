package pt.ipc.http.models.emitter

import java.util.UUID

data class PostedVideo(val patientID: UUID, val name: String, val exerciseID: Int) : EmitterModel()
