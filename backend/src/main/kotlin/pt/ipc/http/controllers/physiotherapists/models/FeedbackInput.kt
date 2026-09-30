package pt.ipc.http.controllers.physiotherapists.models

data class FeedbackInput(
    val set: Int,
    val feedback: String,
    val feedbackScore: String? = null
)
