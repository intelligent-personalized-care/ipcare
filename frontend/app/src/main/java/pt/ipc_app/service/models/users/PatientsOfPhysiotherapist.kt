package pt.ipc_app.service.models.users

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.*

data class PatientOfPhysiotherapist(
    val id: UUID,
    val name: String,
    val email: String,
    val weight: Int? = null,
    val height: Int? = null,
    val physicalCondition: String? = null,
    val birthDate: String? = null,
    val plans: List<PlanOfPatient> = emptyList()
)

data class PlanOfPatient(
    val id: Int,
    val title: String,
    val startDate: String,
    val endDate: String
)

@Parcelize
data class PatientsOfPhysiotherapist(
    val patients: List<PatientOutput>
): Parcelable