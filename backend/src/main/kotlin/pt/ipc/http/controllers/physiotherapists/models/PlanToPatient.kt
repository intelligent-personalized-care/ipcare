package pt.ipc.http.controllers.physiotherapists.models

import java.time.LocalDate

data class PlanToPatient(val planID: Int, val startDate: LocalDate)
