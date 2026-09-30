package pt.ipc.http.controllers.physiotherapists.models

import pt.ipc.domain.patient.PatientDailyExercises

data class ExercisesOfPatients(val patientsExercises: List<PatientDailyExercises>)
