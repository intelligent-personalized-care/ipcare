package pt.ipc_app.service.models.register

data class RegisterPatientInput(
    val name: String,
    val email: String,
    val password: String,
    val weight : Int?,
    val height : Int?,
    val birthDate : String?,
    val physicalCondition : String?
)

data class RegisterPhysiotherapistInput(
    val name: String,
    val email: String,
    val password: String
)
