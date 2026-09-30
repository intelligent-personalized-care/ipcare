package pt.ipc.services.physiotherapistService

import org.springframework.stereotype.Service
import pt.ipc.domain.Role
import pt.ipc.domain.User
import pt.ipc.domain.patient.PatientDailyExercises
import pt.ipc.domain.patient.PatientInformation
import pt.ipc.domain.patient.PatientOfPhysiotherapist
import pt.ipc.domain.encryption.EncryptionUtils
import pt.ipc.domain.exceptions.PatientAlreadyHavePlanInThisPeriod
import pt.ipc.domain.exceptions.HasNotUploadedVideo
import pt.ipc.domain.exceptions.PhysiotherapistNotFound
import pt.ipc.domain.exceptions.NotPhysiotherapistOfPatient
import pt.ipc.domain.exceptions.NotPlanOfPhysiotherapist
import pt.ipc.domain.exceptions.PlanNotFound
import pt.ipc.domain.exceptions.RequestNotExists
import pt.ipc.domain.exceptions.UserNotExists
import pt.ipc.domain.physiotherapist.PhysiotherapistDetails
import pt.ipc.domain.physiotherapist.PhysiotherapistProfile
import pt.ipc.domain.physiotherapist.RequestInformation
import pt.ipc.domain.plan.PlanInfoOutput
import pt.ipc.domain.plan.PlanInput
import pt.ipc.domain.plan.PlanOutput
import pt.ipc.services.ServiceUtils
import pt.ipc.services.dtos.CredentialsOutput
import pt.ipc.services.dtos.PhysiotherapistOutput
import pt.ipc.services.dtos.RegisterInput
import pt.ipc.storage.transaction.TransactionManager
import java.time.LocalDate
import java.util.*

@Service
class PhysiotherapistsServiceImpl(
    private val encryptionUtils: EncryptionUtils,
    private val transactionManager: TransactionManager,
    private val serviceUtils: ServiceUtils
) : PhysiotherapistService {

    override fun registerPhysiotherapist(registerInput: RegisterInput): CredentialsOutput {
        serviceUtils.checkDetails(email = registerInput.email, password = registerInput.password)

        val (userID, accessToken, refreshToken, sessionID) = serviceUtils.createCredentials(role = Role.PHYSIOTHERAPIST)

        val user = User(
            id = userID,
            name = registerInput.name,
            email = registerInput.email,
            passwordHash = encryptionUtils.encrypt(registerInput.password)
        )

        val encryptedSession = encryptionUtils.encrypt(plainText = sessionID.toString())

        transactionManager.run {
            it.physiotherapistRepository.registerPhysiotherapist(user = user, sessionID = encryptedSession)
        }

        return CredentialsOutput(id = userID, accessToken = accessToken, refreshToken = refreshToken)
    }

    override fun insertCredential(physiotherapistID: UUID, credential: ByteArray) {
        transactionManager.run(fileName = physiotherapistID) {
            it.cloudStorage.uploadPhysiotherapistCredentials(fileName = physiotherapistID, file = credential)
            it.physiotherapistRepository.insertCredential(physiotherapistID = physiotherapistID, dtSubmit = LocalDate.now())
        }
    }

    override fun getPhysiotherapist(physiotherapistID: UUID): PhysiotherapistDetails =
        transactionManager.run {
            it.physiotherapistRepository.getPhysiotherapist(physiotherapistID = physiotherapistID) ?: throw PhysiotherapistNotFound
        }

    override fun getPatientsOfPhysiotherapist(physiotherapistID: UUID): List<PatientInformation> =
        transactionManager.run {
            it.physiotherapistRepository.getPatientsOfPhysiotherapist(physiotherapistID = physiotherapistID)
        }

    override fun getPatientOfPhysiotherapist(physiotherapistID: UUID, patientID: UUID): PatientOfPhysiotherapist =
        transactionManager.run {
            if (!it.physiotherapistRepository.isPhysiotherapistOfPatient(
                    physiotherapistID = physiotherapistID,
                    patientID = patientID
                )
            ) {
                throw NotPhysiotherapistOfPatient
            }
            it.physiotherapistRepository.getPatientOfPhysiotherapist(physiotherapistID = physiotherapistID, patientID = patientID) ?: throw UserNotExists
        }

    override fun updateProfilePicture(physiotherapistID: UUID, photo: ByteArray) {
        transactionManager.run(fileName = physiotherapistID) {
            it.cloudStorage.uploadProfilePicture(fileName = physiotherapistID, file = photo)
        }
    }

    override fun getPhysiotherapistProfile(physiotherapistID: UUID): PhysiotherapistProfile =
        transactionManager.run {
            it.physiotherapistRepository.getPhysiotherapistProfile(physiotherapistID = physiotherapistID) ?: throw UserNotExists
        }

    override fun getProfilePicture(physiotherapistID: UUID): ByteArray {
        return transactionManager.run {
            it.cloudStorage.downloadProfilePicture(fileName = physiotherapistID)
        }
    }

    override fun physiotherapistRequests(physiotherapistID: UUID): List<RequestInformation> =
        transactionManager.run {
            it.physiotherapistRepository.physiotherapistRequests(physiotherapistID = physiotherapistID)
        }

    override fun decideRequest(requestID: UUID, physiotherapistID: UUID, decision: Boolean): Triple<List<PatientInformation>, UUID, PhysiotherapistOutput>? =
        transactionManager.run {
            val requestInformation =
                it.physiotherapistRepository.getRequestInformation(requestID = requestID) ?: throw RequestNotExists

            if (!decision) {
                it.physiotherapistRepository.declineRequest(requestID = requestID)
                return@run null
            }

            it.physiotherapistRepository.acceptRequest(
                requestID = requestID,
                patientID = requestInformation.patientID,
                physiotherapistID = physiotherapistID
            )

            val patients = it.physiotherapistRepository.getPatientsOfPhysiotherapist(physiotherapistID = physiotherapistID)

            val details = it.physiotherapistRepository.getPhysiotherapistOfPatient(requestInformation.patientID) ?: throw PhysiotherapistNotFound
            val stars = it.physiotherapistRepository.getPhysiotherapistRating(details.id)

            val physiotherapistOutput =
                PhysiotherapistOutput(id = details.id, name = details.name, email = details.email, rating = stars)

            Triple(first = patients, second = requestInformation.patientID, third = physiotherapistOutput)
        }

    override fun deleteConnection(physiotherapistID: UUID, patientID: UUID) {
        transactionManager.run {
            it.physiotherapistRepository.getPatientOfPhysiotherapist(physiotherapistID = physiotherapistID, patientID = patientID) ?: NotPhysiotherapistOfPatient
            it.patientsRepository.deleteConnection(physiotherapistID = physiotherapistID, patientID = patientID)
        }
    }

    override fun createPlan(physiotherapistID: UUID, planInput: PlanInput): Int {
        return transactionManager.run {
            it.plansRepository.createPlan(physiotherapistID = physiotherapistID, plan = planInput)
        }
    }

    override fun associatePlanToPatient(physiotherapistID: UUID, patientID: UUID, startDate: LocalDate, planID: Int): Pair<String, LocalDate> {
        return transactionManager.run {
            if (!it.physiotherapistRepository.isPhysiotherapistOfPatient(
                    physiotherapistID = physiotherapistID,
                    patientID = patientID
                )
            ) {
                throw NotPhysiotherapistOfPatient
            }

            val plan = it.plansRepository.getPlan(planID = planID) ?: throw PlanNotFound

            val endDate = startDate.plusDays((plan.dailyLists.size - 1).toLong())

            if (it.plansRepository.checkIfExistsPlanOfPatientInThisPeriod(
                    patientID = patientID,
                    startDate = startDate,
                    endDate = endDate
                )
            ) {
                throw PatientAlreadyHavePlanInThisPeriod
            }

            it.plansRepository.associatePlanToPatient(
                planID = planID,
                patientID = patientID,
                startDate = startDate,
                endDate = endDate
            )

            Pair(first = plan.title, second = startDate)
        }
    }

    override fun getPlan(physiotherapistID: UUID, planID: Int): PlanOutput {
        return transactionManager.run {
            if (!it.plansRepository.checkIfPlanIsOfPhysiotherapist(
                    physiotherapistID = physiotherapistID,
                    planID = planID
                )
            ) {
                throw NotPlanOfPhysiotherapist
            }
            it.plansRepository.getPlanOfPhysiotherapist(planID) ?: throw PlanNotFound
        }
    }

    override fun getPlans(physiotherapistID: UUID): List<PlanInfoOutput> {
        return transactionManager.run {
            it.plansRepository.getPlans(physiotherapistID = physiotherapistID)
        }
    }

    override fun giveFeedbackOfExercise(
        physiotherapistID: UUID,
        planID: Int,
        dailyListID: Int,
        dailyExerciseID: Int,
        set: Int,
        feedback: String,
        feedbackScore: String?,
        patientID: UUID
    ) {
        pt.ipc.domain.exercises.validateFeedbackScore(feedbackScore)
        return transactionManager.run {
            if (!it.physiotherapistRepository.isPhysiotherapistOfPatient(
                    physiotherapistID = physiotherapistID,
                    patientID = patientID
                )
            ) {
                throw NotPhysiotherapistOfPatient
            }

            if (!it.plansRepository.checkIfPatientAlreadyUploadedVideo(
                    patientID = patientID,
                    planID = planID,
                    dailyListID = dailyListID,
                    exerciseID = dailyExerciseID,
                    set = set
                )
            ) {
                throw HasNotUploadedVideo
            }

            if (!it.plansRepository.checkIfPhysiotherapistHasPrescribedExercise(
                    planID = planID,
                    exerciseID = dailyExerciseID,
                    physiotherapistID = physiotherapistID
                )
            ) {
                throw NotPlanOfPhysiotherapist
            }

            it.plansRepository.giveFeedBackOfVideo(
                patientID = patientID,
                exerciseID = dailyExerciseID,
                set = set,
                feedBack = feedback,
                feedbackScore = feedbackScore
            )
        }
    }

    override fun exercisesOfPatients(physiotherapistID: UUID, date: LocalDate): List<PatientDailyExercises> =
        transactionManager.run {
            it.physiotherapistRepository.exercisesOfPatients(physiotherapistID = physiotherapistID, date = date)
        }
}
