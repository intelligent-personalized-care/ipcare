package pt.ipc_app.domain.user

import com.google.gson.annotations.SerializedName

enum class Role {
    @SerializedName("PATIENT")
    PATIENT,
    @SerializedName("PHYSIOTHERAPIST")
    PHYSIOTHERAPIST;

    companion object {
        fun isPatient(role: String) = role == PATIENT.name
    }
}

fun Role.isPatient(): Boolean = this == Role.PATIENT

fun String.toRole() = when (this) {
    "PATIENT" -> Role.PATIENT
    "PHYSIOTHERAPIST" -> Role.PHYSIOTHERAPIST
    else -> null
}
