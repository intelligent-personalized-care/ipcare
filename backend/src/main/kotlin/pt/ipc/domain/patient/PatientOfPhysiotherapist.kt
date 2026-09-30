package pt.ipc.domain.patient

import pt.ipc.domain.plan.PlanOfPatient
import java.time.LocalDate
import java.util.UUID

data class PatientOfPhysiotherapist(
    val id: UUID,
    val name: String,
    val email: String,
    val weight: Int? = null,
    val height: Int? = null,
    val physicalCondition: String? = null,
    val birthDate: LocalDate? = null,
    val plans: List<PlanOfPatient>
)
