package pt.ipc.domain

enum class Role {
    PATIENT,
    PHYSIOTHERAPIST,
    ADMIN;

    fun isPhysiotherapist(): Boolean = this == PHYSIOTHERAPIST

    fun isPatient(): Boolean = this == PATIENT

    fun notAdmin(): Boolean = this != ADMIN

    fun notPatient(): Boolean = !isPatient()

    fun notPhysiotherapist(): Boolean = !isPhysiotherapist()
}

fun Any?.toRole(): Role =
    when (this) {
        "PATIENT" -> Role.PATIENT
        "PHYSIOTHERAPIST" -> Role.PHYSIOTHERAPIST
        "ADMIN" -> Role.ADMIN
        else -> throw pt.ipc.domain.exceptions.Unauthenticated
    }
