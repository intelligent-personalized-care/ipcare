package pt.ipc_app.service.models.sse

import pt.ipc_app.service.models.users.PhysiotherapistOutput

data class RequestAcceptance(
    val physiotherapist: PhysiotherapistOutput
): SseEvent()