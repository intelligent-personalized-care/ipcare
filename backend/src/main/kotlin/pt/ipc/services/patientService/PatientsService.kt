package pt.ipc.services.patientService

import pt.ipc.domain.patient.PatientOutput
import pt.ipc.domain.exercises.Exercise
import pt.ipc.domain.physiotherapist.PhysiotherapistAvailable
import pt.ipc.http.controllers.patients.models.RegisterPatientInput
import pt.ipc.http.models.emitter.PostedVideo
import pt.ipc.http.models.emitter.RequestPhysiotherapist
import pt.ipc.services.dtos.CredentialsOutput
import pt.ipc.services.dtos.PhysiotherapistOutput
import java.time.LocalDate
import java.util.*

interface PatientsService {

    fun registerPatient(input: RegisterPatientInput): CredentialsOutput

    fun addProfilePicture(patientID: UUID, profilePicture: ByteArray)

    fun getPatientProfile(patientID: UUID): PatientOutput

    fun deleteConnection(patientID: UUID)

    fun searchPhysiotherapistsAvailable(patientID: UUID, name: String?, skip: Int, limit: Int): List<PhysiotherapistAvailable>

    fun requestPhysiotherapist(physiotherapistID: UUID, patientID: UUID, requestText: String?): RequestPhysiotherapist

    fun getPhysiotherapistOfPatient(patientID: UUID): PhysiotherapistOutput

    fun getExercisesOfPatient(patientID: UUID, date: LocalDate?, skip: Int, limit: Int): List<Exercise>

    fun ratePhysiotherapist(physiotherapistID: UUID, patientID: UUID, rating: Int)

    fun uploadVideoOfPatient(
        video: ByteArray,
        patientID: UUID,
        planID: Int,
        dailyListID: Int,
        exerciseID: Int,
        set: Int,
        feedback: String? = null,
        executionMode: String? = null,
        withLoad: Boolean? = null,
        loadValue: Float? = null,
        loadUnit: String? = null,
        executionScore: String? = null
    ): Pair<UUID, PostedVideo?>
}
