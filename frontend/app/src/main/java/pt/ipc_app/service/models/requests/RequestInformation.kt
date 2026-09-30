package pt.ipc_app.service.models.requests

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class RequestsOfPhysiotherapist(
    val requests: List<RequestInformation>
): Parcelable

@Parcelize
data class RequestInformation(
    val requestID: UUID,
    val requestText: String? = null,
    val patientID: UUID,
    val patientName: String,
    val patientEmail: String
): Parcelable
