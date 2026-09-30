package pt.ipc.domain.plan

import java.time.LocalDate

data class PlanOfPatient(
    val id: Int,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate
)
