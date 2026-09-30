package pt.ipc_app.service.models.exercises

data class FeedbackInput(val set: Int, val feedback: String, val feedbackScore: String? = null)
