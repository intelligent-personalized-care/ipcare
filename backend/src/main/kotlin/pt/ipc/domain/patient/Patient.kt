package pt.ipc.domain.patient

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Patient(
    val id: UUID,
    val name: String,
    val email: String,
    val password: String,
    val weight: Int? = null,
    val height: Int? = null,
    val physicalCondition: String? = null,
    val birthDate: LocalDate? = null
)

fun String.toLocalDate(): LocalDate = LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)

data class PatientOutput(
    val id: UUID,
    val name: String,
    val email: String,
    val weight: Int? = null,
    val height: Int? = null,
    val physicalCondition: String? = null,
    val birthDate: LocalDate? = null
)
