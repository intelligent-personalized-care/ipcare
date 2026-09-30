package pt.ipc.services.exercisesService

import pt.ipc.domain.exercises.ExerciseInfo
import pt.ipc.domain.exercises.ExerciseType
import pt.ipc.domain.plan.PlanOutput
import pt.ipc.domain.plan.VideoFeedBack
import java.time.LocalDate
import java.util.UUID

interface ExercisesService {

    fun getExercisesInfo(exerciseID: UUID): ExerciseInfo

    fun getExercises(exerciseType: String?, skip: Int, limit: Int, joint: String? = null): List<ExerciseInfo>

    fun getExercisePreviewVideo(exerciseID: UUID): ByteArray

    fun getPatientVideo(patientID: UUID, userID: UUID, planID: Int, dailyList: Int, dailyExercise: Int, set: Int): ByteArray

    fun getVideoFeedback(patientID: UUID, userID: UUID, planID: Int, dailyList: Int, dailyExercise: Int, set: Int): VideoFeedBack

    fun getPlanOfPatientContainingDate(userID: UUID, patientID: UUID, date: LocalDate): PlanOutput
}
