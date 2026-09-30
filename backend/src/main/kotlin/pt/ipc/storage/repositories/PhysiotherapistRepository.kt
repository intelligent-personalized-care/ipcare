package pt.ipc.storage.repositories

import pt.ipc.domain.User
import pt.ipc.domain.patient.PatientDailyExercises
import pt.ipc.domain.patient.PatientInformation
import pt.ipc.domain.patient.PatientOfPhysiotherapist
import pt.ipc.domain.physiotherapist.PhysiotherapistAvailable
import pt.ipc.domain.physiotherapist.PhysiotherapistDetails
import pt.ipc.domain.physiotherapist.PhysiotherapistProfile
import pt.ipc.domain.physiotherapist.Rating
import pt.ipc.domain.physiotherapist.RequestInformation
import java.time.LocalDate
import java.util.UUID

interface PhysiotherapistRepository {

    fun registerPhysiotherapist(user: User, sessionID: String)

    fun insertCredential(physiotherapistID: UUID, dtSubmit: LocalDate)

    fun getUserByIDAndSession(id: UUID, sessionID: String): User?

    fun getPhysiotherapistProfile(physiotherapistID: UUID): PhysiotherapistProfile?

    fun getPatientOfPhysiotherapist(physiotherapistID: UUID, patientID: UUID): PatientOfPhysiotherapist?

    fun getPhysiotherapist(physiotherapistID: UUID): PhysiotherapistDetails?

    fun getPatientsOfPhysiotherapist(physiotherapistID: UUID): List<PatientInformation>

    fun getPhysiotherapistOfPatient(patientID: UUID): PhysiotherapistDetails?

    fun getPhysiotherapistRating(physiotherapistID: UUID): Rating

    fun searchPhysiotherapistsAvailable(name: String?, skip: Int, limit: Int, patientID: UUID): List<PhysiotherapistAvailable>

    fun acceptRequest(requestID: UUID, patientID: UUID, physiotherapistID: UUID)

    fun declineRequest(requestID: UUID)

    fun getRequestInformation(requestID: UUID): RequestInformation?

    fun physiotherapistRequests(physiotherapistID: UUID): List<RequestInformation>

    fun checkIfPhysiotherapistIsVerified(physiotherapistID: UUID): Boolean

    fun isPhysiotherapistOfPatient(physiotherapistID: UUID, patientID: UUID): Boolean

    fun exercisesOfPatients(physiotherapistID: UUID, date: LocalDate): List<PatientDailyExercises>

    fun getAllCredentials(): List<UUID>

    fun deleteCredential(physiotherapistID: UUID)
}

