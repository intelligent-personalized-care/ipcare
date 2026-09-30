package pt.ipc.storage.repositories

import pt.ipc.domain.patient.Patient
import pt.ipc.domain.patient.PatientOutput
import java.time.LocalDate
import java.util.*

interface PatientsRepository {

    fun existsEmail(email: String): Boolean

    fun getPatient(patientID: UUID): PatientOutput?

    fun registerPatient(input: Patient, sessionID: String)

    fun requestPhysiotherapist(requestID: UUID, physiotherapistID: UUID, patientID: UUID, requestText: String? = null)

    fun deleteConnection(physiotherapistID: UUID, patientID: UUID)

    fun hasPatientRatedPhysiotherapist(patientID: UUID, physiotherapistID: UUID): Boolean

    fun ratePhysiotherapist(patientID: UUID, physiotherapistID: UUID, rating: Int)

    fun checkIfPatientHasThisExercise(patientID: UUID, planID: Int, dailyList: Int, exerciseID: Int): Boolean

    fun checkIfPatientAlreadyUploadedVideo(patientID: UUID, exerciseID: Int, set: Int): Boolean

    fun uploadExerciseVideoOfPatient(
        patientID: UUID,
        exerciseID: Int,
        exerciseVideoID: UUID,
        date: LocalDate,
        set: Int,
        patientFeedback: String?,
        executionMode: String? = null,
        withLoad: Boolean? = null,
        loadValue: Float? = null,
        loadUnit: String? = null,
        executionScore: String? = null
    ): Boolean

    fun getPatientsVideosIDs(): List<UUID>

    fun deletePatientVideoID(videoID: UUID)
}
