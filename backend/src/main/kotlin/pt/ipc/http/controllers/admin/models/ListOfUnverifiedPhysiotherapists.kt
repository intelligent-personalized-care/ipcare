package pt.ipc.http.controllers.admin.models

import pt.ipc.services.dtos.PhysiotherapistInfo

data class ListOfUnverifiedPhysiotherapists(val physiotherapists: List<PhysiotherapistInfo>)
