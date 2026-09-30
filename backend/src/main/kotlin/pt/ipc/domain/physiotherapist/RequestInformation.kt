package pt.ipc.domain.physiotherapist

import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.util.UUID

data class RequestInformation(
    val requestID: UUID,
    val requestText: String? = null,
    @ColumnName("patient_id") val patientID: UUID,
    @ColumnName("name") val patientName: String,
    @ColumnName("email") val patientEmail: String
)
