package pt.ipc.domain.patient

import pt.ipc.domain.exercises.DailyExercise
import java.util.UUID

data class PatientDailyExercises(
    val id: UUID,
    val name: String,
    val planId: Int,
    val dailyListId: Int,
    val exercises: List<DailyExercise>
)
