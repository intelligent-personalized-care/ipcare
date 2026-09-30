package pt.ipc.http.controllers.patients.models

import pt.ipc.domain.physiotherapist.PhysiotherapistAvailable

data class AllPhysiotherapistsAvailableOutput(
    val physiotherapists: List<PhysiotherapistAvailable>
)
