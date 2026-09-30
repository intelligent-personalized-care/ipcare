package pt.ipc.http.models.emitter

data class PhysiotherapistFeedBack(val feedBack: String, val feedbackScore: String? = null, val exerciseId: Int? = null, val set: Int? = null) : EmitterModel()
