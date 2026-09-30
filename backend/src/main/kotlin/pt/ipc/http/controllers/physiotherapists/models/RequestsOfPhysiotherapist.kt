package pt.ipc.http.controllers.physiotherapists.models

import pt.ipc.domain.physiotherapist.RequestInformation

data class RequestsOfPhysiotherapist(
    val requests: List<RequestInformation>
)
