package pt.ipc.services.physiotherapistService

import pt.ipc.domain.patient.PatientDailyExercises
import pt.ipc.domain.patient.PatientInformation
import pt.ipc.domain.patient.PatientOfPhysiotherapist
import pt.ipc.domain.physiotherapist.PhysiotherapistDetails
import pt.ipc.domain.physiotherapist.PhysiotherapistProfile
import pt.ipc.domain.physiotherapist.RequestInformation
import pt.ipc.domain.plan.PlanInfoOutput
import pt.ipc.domain.plan.PlanInput
import pt.ipc.domain.plan.PlanOutput
import pt.ipc.services.dtos.CredentialsOutput
import pt.ipc.services.dtos.PhysiotherapistOutput
import pt.ipc.services.dtos.RegisterInput
import java.time.LocalDate
import java.util.*

interface PhysiotherapistService {

    fun registerPhysiotherapist(registerInput: RegisterInput): CredentialsOutput

    fun insertCredential(physiotherapistID: UUID, credential: ByteArray)

    fun getPhysiotherapist(physiotherapistID: UUID): PhysiotherapistDetails

    fun getPatientsOfPhysiotherapist(physiotherapistID: UUID): List<PatientInformation>

    fun getPatientOfPhysiotherapist(physiotherapistID: UUID, patientID: UUID): PatientOfPhysiotherapist

    fun updateProfilePicture(physiotherapistID: UUID, photo: ByteArray)

    fun getPhysiotherapistProfile(physiotherapistID: UUID): PhysiotherapistProfile

    fun getProfilePicture(physiotherapistID: UUID): ByteArray

    fun physiotherapistRequests(physiotherapistID: UUID): List<RequestInformation>

    fun decideRequest(requestID: UUID, physiotherapistID: UUID, decision: Boolean): Triple<List<PatientInformation>, UUID, PhysiotherapistOutput>?

    fun deleteConnection(physiotherapistID: UUID, patientID: UUID)

    fun createPlan(physiotherapistID: UUID, planInput: PlanInput): Int

    fun associatePlanToPatient(physiotherapistID: UUID, patientID: UUID, startDate: LocalDate, planID: Int): Pair<String, LocalDate>

    fun getPlan(physiotherapistID: UUID, planID: Int): PlanOutput

    fun getPlans(physiotherapistID: UUID): List<PlanInfoOutput>

    fun giveFeedbackOfExercise(
        physiotherapistID: UUID,
        planID: Int,
        dailyListID: Int,
        dailyExerciseID: Int,
        set: Int,
        feedback: String,
        feedbackScore: String? = null,
        patientID: UUID
    )

    fun exercisesOfPatients(physiotherapistID: UUID, date: LocalDate): List<PatientDailyExercises>
}

