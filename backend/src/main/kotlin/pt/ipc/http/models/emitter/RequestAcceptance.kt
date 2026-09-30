package pt.ipc.http.models.emitter

import pt.ipc.services.dtos.PhysiotherapistOutput

data class RequestAcceptance(val physiotherapist: PhysiotherapistOutput) : EmitterModel()
