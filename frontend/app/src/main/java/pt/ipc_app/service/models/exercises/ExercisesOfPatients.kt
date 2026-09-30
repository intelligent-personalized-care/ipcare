package pt.ipc_app.service.models.exercises

import pt.ipc_app.domain.exercise.DailyExercise
import java.util.*

data class ExercisesOfPatients(
    val patientsExercises: List<PatientDailyExercises>
)

data class PatientDailyExercises(
    val id: UUID,
    val name: String,
    val planId: Int,
    val dailyListId: Int,
    val exercises: List<DailyExercise>
) {
    fun allExercisesDone() = exercises.all { it.isDone }
}
