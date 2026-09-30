package pt.ipc.services.exercisesService

import org.springframework.stereotype.Service
import pt.ipc.domain.exceptions.PatientDontHavePlan
import pt.ipc.domain.exceptions.PatientNotPostedVideo
import pt.ipc.domain.exceptions.ExerciseNotExists
import pt.ipc.domain.exceptions.ForbiddenRequest
import pt.ipc.domain.exercises.ExerciseInfo
import pt.ipc.domain.exercises.ExerciseType
import pt.ipc.domain.plan.PlanOutput
import pt.ipc.domain.plan.VideoFeedBack
import pt.ipc.storage.transaction.TransactionManager
import java.time.LocalDate
import java.util.UUID

@Service
class ExercisesServiceImpl(
    private val transactionManager: TransactionManager
) : ExercisesService {
    override fun getExercisesInfo(exerciseID: UUID): ExerciseInfo {
        return transactionManager.run {
            it.exerciseRepository.getExercise(exerciseID = exerciseID) ?: throw ExerciseNotExists
        }
    }

    override fun getExercises(exerciseType: String?, skip: Int, limit: Int, joint: String?): List<ExerciseInfo> {
        if (joint != null && joint !in setOf("WRIST", "ELBOW", "KNEE")) throw object : pt.ipc.domain.exceptions.BadRequest("Invalid joint") {}
        if (skip < 0 || limit !in 1..100) throw object : pt.ipc.domain.exceptions.BadRequest("Invalid pagination") {}
        val type = if (exerciseType != null) ExerciseType.values().firstOrNull { it.name.contains(exerciseType) } else null
        return transactionManager.run {
            if (type == null) {
                it.exerciseRepository.getExercises(skip = skip, limit = limit, joint = joint)
            } else {
                it.exerciseRepository.getExerciseByType(type = type, skip = skip, limit = limit, joint = joint)
            }
        }
    }

    override fun getExercisePreviewVideo(exerciseID: UUID): ByteArray {
        return transactionManager.run {
            it.cloudStorage.downloadExampleVideo(exerciseID = exerciseID)
        }
    }

    override fun getPatientVideo(patientID: UUID, userID: UUID, planID: Int, dailyList: Int, dailyExercise: Int, set: Int): ByteArray {
        return transactionManager.run {
            if (userID != patientID) {
                if (!it.physiotherapistRepository.isPhysiotherapistOfPatient(physiotherapistID = userID, patientID = patientID)) throw ForbiddenRequest
            }

            val videoID = it.exerciseRepository.getPatientVideoID(
                patientID = patientID,
                planID = planID,
                dailyListID = dailyList,
                dailyExerciseID = dailyExercise,
                set = set
            ) ?: throw PatientNotPostedVideo

            it.cloudStorage.downloadPatientVideo(fileName = videoID)
        }
    }

    override fun getVideoFeedback(
        patientID: UUID,
        userID: UUID,
        planID: Int,
        dailyList: Int,
        dailyExercise: Int,
        set: Int
    ): VideoFeedBack {
        return transactionManager.run {
            if (userID != patientID) {
                if (!it.physiotherapistRepository.isPhysiotherapistOfPatient(
                        physiotherapistID = userID,
                        patientID = patientID
                    )
                ) {
                    throw ForbiddenRequest
                }
            }

            val videoID = it.exerciseRepository.getPatientVideoID(
                patientID = patientID,
                planID = planID,
                dailyListID = dailyList,
                dailyExerciseID = dailyExercise,
                set = set
            ) ?: throw PatientNotPostedVideo

            it.exerciseRepository.getVideoFeedback(videoID = videoID)
        }
    }

    override fun getPlanOfPatientContainingDate(userID: UUID, patientID: UUID, date: LocalDate): PlanOutput =
        transactionManager.run {
            if (userID != patientID) {
                if (!it.physiotherapistRepository.isPhysiotherapistOfPatient(
                        physiotherapistID = userID,
                        patientID = patientID
                    )
                ) {
                    throw ForbiddenRequest
                }
            }

            it.plansRepository.getPlanOfPatientContainingDate(patientID = patientID, date = date)
                ?: throw PatientDontHavePlan
        }
}
