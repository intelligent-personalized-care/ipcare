package pt.ipc_app.service.models.exercises

import com.google.gson.annotations.SerializedName

data class ExerciseVideoFeedback(
    @SerializedName("patientFeedBack")
    val patientFeedBack: String?,
    @SerializedName("physiotherapistFeedBack")
    val physiotherapistFeedBack: String?,
    val executionMode: String? = null,
    val withLoad: Boolean? = null,
    val loadValue: Float? = null,
    val loadUnit: String? = null,
    val physiotherapistFeedbackScore: String? = null
)
