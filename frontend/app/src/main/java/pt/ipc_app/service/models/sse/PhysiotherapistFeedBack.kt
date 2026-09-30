package pt.ipc_app.service.models.sse

data class PhysiotherapistFeedBack(
    val feedBack: String,
    val feedbackScore: String? = null,
    val exerciseId: Int? = null,
    val set: Int? = null
): SseEvent()