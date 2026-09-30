package pt.ipc.services.patientService

import org.springframework.stereotype.Service
import pt.ipc.domain.Role
import pt.ipc.domain.patient.Patient
import pt.ipc.domain.patient.PatientOutput
import pt.ipc.domain.patient.toLocalDate
import pt.ipc.domain.encryption.EncryptionUtils
import pt.ipc.domain.exceptions.AlreadyRatedThisPhysiotherapist
import pt.ipc.domain.exceptions.PatientAlreadyHavePhysiotherapist
import pt.ipc.domain.exceptions.PatientDontHaveThisExercise
import pt.ipc.domain.exceptions.ExerciseAlreadyUploaded
import pt.ipc.domain.exceptions.PhysiotherapistNotFound
import pt.ipc.domain.exceptions.NotPhysiotherapistOfPatient
import pt.ipc.domain.exceptions.UserNotExists
import pt.ipc.domain.exercises.Exercise
import pt.ipc.domain.physiotherapist.PhysiotherapistAvailable
import pt.ipc.http.controllers.patients.models.RegisterPatientInput
import pt.ipc.http.models.emitter.PostedVideo
import pt.ipc.http.models.emitter.RequestPhysiotherapist
import pt.ipc.services.ServiceUtils
import pt.ipc.services.dtos.CredentialsOutput
import pt.ipc.services.dtos.PhysiotherapistOutput
import pt.ipc.storage.transaction.TransactionManager
import java.time.LocalDate
import java.util.UUID

@Service
class PatientsServiceImpl(
    private val transactionManager: TransactionManager,
    private val encryptionUtils: EncryptionUtils,
    private val serviceUtils: ServiceUtils
) : PatientsService {

    override fun registerPatient(input: RegisterPatientInput): CredentialsOutput {
        serviceUtils.checkDetails(email = input.email, password = input.password)

        val (userID, accessToken, refreshToken, sessionID) = serviceUtils.createCredentials(role = Role.PATIENT)

        val encryptedSession = encryptionUtils.encrypt(plainText = sessionID.toString())

        val encryptedPatient = Patient(
            id = userID,
            name = input.name,
            email = input.email,
            password = encryptionUtils.encrypt(input.password),
            weight = input.weight,
            height = input.height,
            physicalCondition = input.physicalCondition,
            birthDate = input.birthDate?.toLocalDate()
        )

        transactionManager.run {
            it.patientsRepository.registerPatient(
                input = encryptedPatient,
                sessionID = encryptedSession
            )
        }

        return CredentialsOutput(id = userID, accessToken = accessToken, refreshToken = refreshToken)
    }

    override fun addProfilePicture(patientID: UUID, profilePicture: ByteArray) {
        transactionManager.run(fileName = patientID) {
            it.cloudStorage.uploadProfilePicture(fileName = patientID, file = profilePicture)
        }
    }

    override fun getPatientProfile(patientID: UUID): PatientOutput =
        transactionManager.run {
            it.patientsRepository.getPatient(patientID = patientID) ?: throw UserNotExists
        }

    override fun deleteConnection(patientID: UUID) {
        transactionManager.run {
            val physiotherapist = it.physiotherapistRepository.getPhysiotherapistOfPatient(patientID = patientID) ?: throw PhysiotherapistNotFound
            it.patientsRepository.deleteConnection(physiotherapistID = physiotherapist.id, patientID = patientID)
        }
    }

    override fun searchPhysiotherapistsAvailable(patientID: UUID, name: String?, skip: Int, limit: Int): List<PhysiotherapistAvailable> =
        transactionManager.run {
            it.physiotherapistRepository.searchPhysiotherapistsAvailable(name = name, skip = skip, limit = limit, patientID)
        }

    override fun requestPhysiotherapist(physiotherapistID: UUID, patientID: UUID, requestText: String?): RequestPhysiotherapist {
        val requestID = UUID.randomUUID()

        return transactionManager.run {
            val patient = it.patientsRepository.getPatient(patientID = patientID) ?: throw UserNotExists
            if (it.physiotherapistRepository.getPhysiotherapistOfPatient(patientID) != null) throw PatientAlreadyHavePhysiotherapist
            it.patientsRepository.requestPhysiotherapist(
                requestID = requestID,
                physiotherapistID = physiotherapistID,
                patientID = patientID,
                requestText = requestText
            )

            RequestPhysiotherapist(requestID = requestID, name = patient.name, requestText = requestText, patientID = patientID)
        }
    }

    override fun getPhysiotherapistOfPatient(patientID: UUID): PhysiotherapistOutput {
        return transactionManager.run {
            val details = it.physiotherapistRepository.getPhysiotherapistOfPatient(patientID) ?: throw PhysiotherapistNotFound
            val stars = it.physiotherapistRepository.getPhysiotherapistRating(details.id)
            PhysiotherapistOutput(id = details.id, name = details.name, email = details.email, rating = stars,
                hasRated = it.patientsRepository.hasPatientRatedPhysiotherapist(patientID, details.id))
        }
    }

    override fun getExercisesOfPatient(patientID: UUID, date: LocalDate?, skip: Int, limit: Int): List<Exercise> {
        return transactionManager.run {
            if (date == null) {
                it.exerciseRepository.getAllExercisesOfPatient(patientID = patientID, skip = skip, limit = limit)
            } else {
                it.exerciseRepository.getExercisesOfDay(patientID = patientID, date = date)
            }
        }
    }

    override fun ratePhysiotherapist(physiotherapistID: UUID, patientID: UUID, rating: Int) {
        transactionManager.run {
            if (!it.physiotherapistRepository.isPhysiotherapistOfPatient(
                    physiotherapistID = physiotherapistID,
                    patientID = patientID
                )
            ) {
                throw NotPhysiotherapistOfPatient
            }
            if (it.patientsRepository.hasPatientRatedPhysiotherapist(
                    patientID = patientID,
                    physiotherapistID = physiotherapistID
                )
            ) {
                throw AlreadyRatedThisPhysiotherapist
            }
            it.patientsRepository.ratePhysiotherapist(patientID = patientID, physiotherapistID = physiotherapistID, rating = rating)
        }
    }

    override fun uploadVideoOfPatient(
        video: ByteArray,
        patientID: UUID,
        planID: Int,
        dailyListID: Int,
        exerciseID: Int,
        set: Int,
        feedback: String?,
        executionMode: String?,
        withLoad: Boolean?,
        loadValue: Float?,
        loadUnit: String?,
        executionScore: String?
    ): Pair<UUID, PostedVideo?> {
        pt.ipc.domain.exercises.validateExecutionLoad(withLoad, loadValue, loadUnit)
        val exerciseVideoID = UUID.randomUUID()
        return transactionManager.run(
            fileName = exerciseVideoID
        ) {
            val physiotherapist = it.physiotherapistRepository.getPhysiotherapistOfPatient(patientID = patientID) ?: throw PhysiotherapistNotFound
            val patient = it.patientsRepository.getPatient(patientID = patientID) ?: throw UserNotExists

            if (!it.patientsRepository.checkIfPatientHasThisExercise(
                    patientID = patientID,
                    planID = planID,
                    dailyList = dailyListID,
                    exerciseID = exerciseID
                )
            ) {
                throw PatientDontHaveThisExercise
            }
            if (it.patientsRepository.checkIfPatientAlreadyUploadedVideo(
                    patientID = patientID,
                    exerciseID = exerciseID,
                    set = set
                )
            ) {
                throw ExerciseAlreadyUploaded
            }

            val hasDone =
                it.patientsRepository.uploadExerciseVideoOfPatient(
                    patientID = patientID,
                    exerciseID = exerciseID,
                    exerciseVideoID = exerciseVideoID,
                    date = LocalDate.now(),
                    patientFeedback = feedback,
                    set = set,
                    executionMode = executionMode,
                    withLoad = withLoad,
                    loadValue = loadValue,
                    loadUnit = loadUnit,
                    executionScore = executionScore
                )

            it.cloudStorage.uploadPatientVideo(fileName = exerciseVideoID, video = video)

            Pair(
                first = physiotherapist.id,
                second = if (hasDone) {
                    PostedVideo(
                        patientID = patientID,
                        name = patient.name,
                        exerciseID = exerciseID
                    )
                } else {
                    null
                }
            )
        }
    }
}

