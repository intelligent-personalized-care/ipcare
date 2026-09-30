package pt.ipc.domain.plan

import org.jdbi.v3.core.mapper.reflect.ColumnName

data class VideoFeedBack(
    @ColumnName("patient_feedback") val patientFeedBack: String?,
    @ColumnName("physiotherapist_feedback") val physiotherapistFeedBack: String?,
    val executionMode: String? = null,
    val withLoad: Boolean? = null,
    val loadValue: Float? = null,
    val loadUnit: String? = null,
    val executionScore: String? = null,
    val physiotherapistFeedbackScore: String? = null
)
